/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.edge.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.foundation.common.cache.VersionedCache;
import org.apache.servicecomb.foundation.common.concurrent.ConcurrentHashMapEx;
import org.apache.servicecomb.foundation.common.net.URIEndpointObject;
import org.apache.servicecomb.foundation.vertx.client.http.HttpClients;
import org.apache.servicecomb.loadbalance.ExtensionsManager;
import org.apache.servicecomb.loadbalance.LoadBalancer;
import org.apache.servicecomb.loadbalance.LoadbalanceHandler;
import org.apache.servicecomb.loadbalance.RuleExt;
import org.apache.servicecomb.loadbalance.ServiceCombServer;
import org.apache.servicecomb.loadbalance.filter.ServerDiscoveryFilter;
import org.apache.servicecomb.registry.RegistrationManager;
import org.apache.servicecomb.registry.discovery.DiscoveryContext;
import org.apache.servicecomb.registry.discovery.DiscoveryTree;
import org.apache.servicecomb.transport.rest.client.Http2TransportHttpClientOptionsSPI;
import org.apache.servicecomb.transport.rest.client.HttpTransportHttpClientOptionsSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.DynamicPropertyFactory;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * This dispatcher forwards requests to any http servers, includes java-chassis providers and other,
 * provided the server is registered to service center.
 *
 * This dispatcher using loadbalance handler to choose the target server. So any functions
 * provided by loadbalancer handler is available, excluding retrying.
 */
public class CommonHttpEdgeDispatcher extends AbstractEdgeDispatcher {
  private static final Logger LOG = LoggerFactory.getLogger(CommonHttpEdgeDispatcher.class);

  private static final String KEY_ENABLED = "servicecomb.http.dispatcher.edge.http.enabled";

  private static final String KEY_ORDER = "servicecomb.http.dispatcher.edge.http.order";

  private static final String KEY_PATTERN = "servicecomb.http.dispatcher.edge.http.pattern";

  private static final String PATTERN_ANY = "/(.*)";

  private static final String KEY_MAPPING_PREFIX = "servicecomb.http.dispatcher.edge.http.mappings";

  private final Map<String, LoadBalancer> loadBalancerMap = new ConcurrentHashMapEx<>();

  private Map<String, URLMappedConfigurationItem> configurations = new HashMap<>();

  private DiscoveryTree discoveryTree;

  public CommonHttpEdgeDispatcher() {
    if (this.enabled()) {
      loadConfigurations();
      discoveryTree = new DiscoveryTree();
      discoveryTree.addFilter(new ServerDiscoveryFilter());
    }
  }

  @Override
  public int getOrder() {
    return DynamicPropertyFactory.getInstance().getIntProperty(KEY_ORDER, 40_000).get();
  }

  @Override
  public boolean enabled() {
    return DynamicPropertyFactory.getInstance().getBooleanProperty(KEY_ENABLED, false).get();
  }

  @Override
  public void init(Router router) {
    String pattern = DynamicPropertyFactory.getInstance().getStringProperty(KEY_PATTERN, PATTERN_ANY).get();
    router.routeWithRegex(pattern).failureHandler(this::onFailure).handler(this::onRequest);
  }

  private void loadConfigurations() {
    ConcurrentCompositeConfiguration config = (ConcurrentCompositeConfiguration) DynamicPropertyFactory
        .getBackingConfigurationSource();
    configurations = URLMappedConfigurationLoader.loadConfigurations(config, KEY_MAPPING_PREFIX);
    config.addConfigurationListener(event -> {
      if (event.getPropertyName().startsWith(KEY_MAPPING_PREFIX)) {
        LOG.info("Map rule have been changed. Reload configurations. Event=" + event.getType());
        configurations = URLMappedConfigurationLoader.loadConfigurations(config, KEY_MAPPING_PREFIX);
      }
    });
  }

  protected void onRequest(RoutingContext context) {
    URLMappedConfigurationItem configurationItem = findConfigurationItem(context.request().uri());
    if (configurationItem == null) {
      context.next();
      return;
    }

    String uri = Utils.findActualPath(context.request().uri(), configurationItem.getPrefixSegmentCount());

    Invocation invocation = new Invocation() {
      @Override
      public String getConfigTransportName() {
        return "rest";
      }

      @Override
      public String getMicroserviceName() {
        return configurationItem.getMicroserviceName();
      }
    };

    LoadBalancer loadBalancer = getOrCreateLoadBalancer(invocation, configurationItem.getMicroserviceName(),
        configurationItem.getVersionRule());
    ServiceCombServer server = loadBalancer.chooseServer(invocation);
    if (server == null) {
      LOG.warn("no available server for service {}", configurationItem.getMicroserviceName());
      serverNotReadyResponse(context);
      return;
    }

    URIEndpointObject endpointObject = new URIEndpointObject(server.getEndpoint().getEndpoint());

    RequestOptions requestOptions = new RequestOptions();
    requestOptions.setHost(endpointObject.getHostOrIp())
        .setPort(endpointObject.getPort())
        .setSsl(endpointObject.isSslEnabled())
        .setMethod(context.request().method())
        .setURI(uri);

    HttpClient httpClient;
    if (endpointObject.isHttp2Enabled()) {
      httpClient = HttpClients.getClient(Http2TransportHttpClientOptionsSPI.CLIENT_NAME, false).getHttpClient();
    } else {
      httpClient = HttpClients.getClient(HttpTransportHttpClientOptionsSPI.CLIENT_NAME, false).getHttpClient();
    }

    httpClient
        .request(requestOptions).compose(httpClientRequest -> {
      context.request().headers().forEach((header) -> {
        httpClientRequest.headers().set(header.getKey(), header.getValue());
      });
      context.request().handler(data -> httpClientRequest.write(data));
      context.request().endHandler((v) -> httpClientRequest.end());

      return httpClientRequest.response().compose(httpClientResponse -> {
        context.response().setStatusCode(httpClientResponse.statusCode());
        httpClientResponse.headers().forEach((header) -> {
          context.response().headers().set(header.getKey(), header.getValue());
        });
        httpClientResponse.handler(this.responseHandler(context, httpClientResponse));
        httpClientResponse.endHandler((v) -> context.response().end());
        return Future.succeededFuture();
      });
    }).onFailure(failure -> {
      LOG.warn("send request to target {}:{} failed, cause {}", endpointObject.getHostOrIp(), endpointObject.getPort(),
          failure.getMessage());
      serverNotReadyResponse(context);
      return;
    });
  }

  private void serverNotReadyResponse(RoutingContext context) {
    context.response().setStatusCode(503);
    context.response().setStatusMessage("service not ready");
    context.response().end();
  }

  protected Handler<Buffer> responseHandler(RoutingContext routingContext, HttpClientResponse httpClientResponse) {
    return data -> routingContext.response().write(data);
  }

  protected LoadBalancer getOrCreateLoadBalancer(Invocation invocation, String microserviceName, String versionRule) {
    DiscoveryContext context = new DiscoveryContext();
    context.setInputParameters(invocation);
    VersionedCache serversVersionedCache = discoveryTree.discovery(context,
        RegistrationManager.INSTANCE.getMicroservice().getAppId(),
        microserviceName,
        versionRule);
    invocation.addLocalContext(LoadbalanceHandler.CONTEXT_KEY_SERVER_LIST, serversVersionedCache.data());
    return loadBalancerMap
        .computeIfAbsent(microserviceName, name -> createLoadBalancer(microserviceName));
  }

  private LoadBalancer createLoadBalancer(String microserviceName) {
    RuleExt rule = ExtensionsManager.createLoadBalancerRule(microserviceName);
    return new LoadBalancer(rule, microserviceName);
  }

  private URLMappedConfigurationItem findConfigurationItem(String path) {
    for (URLMappedConfigurationItem item : configurations.values()) {
      if (item.getPattern().matcher(path).matches()) {
        return item;
      }
    }
    return null;
  }
}
