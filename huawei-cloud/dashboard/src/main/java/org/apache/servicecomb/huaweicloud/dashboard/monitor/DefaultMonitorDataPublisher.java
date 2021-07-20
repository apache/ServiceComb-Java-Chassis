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

package org.apache.servicecomb.huaweicloud.dashboard.monitor;

import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.apache.servicecomb.foundation.auth.SignRequest;
import org.apache.servicecomb.foundation.common.event.EventManager;
import org.apache.servicecomb.foundation.common.net.IpPort;
import org.apache.servicecomb.foundation.common.net.NetUtils;
import org.apache.servicecomb.foundation.ssl.SSLCustom;
import org.apache.servicecomb.foundation.ssl.SSLOption;
import org.apache.servicecomb.foundation.ssl.SSLOptionFactory;
import org.apache.servicecomb.foundation.vertx.AddressResolverConfig;
import org.apache.servicecomb.foundation.vertx.VertxTLSBuilder;
import org.apache.servicecomb.foundation.vertx.VertxUtils;
import org.apache.servicecomb.foundation.vertx.client.ClientPoolManager;
import org.apache.servicecomb.foundation.vertx.client.ClientVerticle;
import org.apache.servicecomb.foundation.vertx.client.http.HttpClientPoolFactory;
import org.apache.servicecomb.foundation.vertx.client.http.HttpClientWithContext;
import org.apache.servicecomb.huaweicloud.dashboard.monitor.data.MonitorConstant;
import org.apache.servicecomb.huaweicloud.dashboard.monitor.event.MonitorFailEvent;
import org.apache.servicecomb.huaweicloud.dashboard.monitor.event.MonitorSuccEvent;
import org.apache.servicecomb.huaweicloud.dashboard.monitor.model.MonitorDaraProvider;
import org.apache.servicecomb.huaweicloud.dashboard.monitor.model.MonitorDataPublisher;
import org.apache.servicecomb.serviceregistry.RegistryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.net.ProxyOptions;

public class DefaultMonitorDataPublisher implements MonitorDataPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMonitorDataPublisher.class);

  private static final String SSL_KEY = "mc.consumer";

  private static final int MAX_WAIT_QUEUE_SIZE = 50;

  private static ClientPoolManager<HttpClientWithContext> clientMgr;

  private AddressManager addressManager;

  @Override
  public void init() {
    try {
      addressManager = new AddressManager();
      deployMonitorClient();
    } catch (Exception e) {
      LOGGER.warn("Deploy monitor data publisher failed will not send monitor data.");
    }
  }

  @Override
  public void publish(MonitorDaraProvider provider) {
    Object data = provider.getData();
    if (data == null) {
      return;
    }
    String endpoint = addressManager.nextServer();
    if (endpoint == null) {
      return;
    }

    String jasonData = Json.encode(data);
    String url = provider.getURL();
    IpPort host = NetUtils.parseIpPortFromURI(endpoint);

    doSend(endpoint, jasonData, url, host, 0);
  }

  private void doSend(String endpoint, String jsonData, String url, IpPort host, int times) {
    clientMgr.findThreadBindClientPool().runOnContext(client -> {
      client.request(HttpMethod.POST, host.getPort(), host.getHostOrIp(), url).compose(request -> {
        request.headers().add("environment", RegistryUtils.getMicroservice().getEnvironment());
        request.setTimeout(MonitorConstant.getInterval() / MonitorConstant.MAX_RETRY_TIMES);
        try {
          SignRequest signReq = SignUtil.createSignRequest(request.getMethod().toString(),
              endpoint + url,
              new HashMap<>(),
              IOUtils.toInputStream(jsonData, "UTF-8"));
          SignUtil.getAuthHeaderProviders().forEach(authHeaderProvider -> {
            request.headers().addAll(authHeaderProvider.getSignAuthHeaders(signReq));
          });
        } catch (Exception e) {
          LOGGER.error("sign request error!", e);
        }
        return request.send(jsonData).compose(rsp -> {
          if (rsp.statusCode() != HttpResponseStatus.OK.code()) {
            if (times < MonitorConstant.MAX_RETRY_TIMES
                && rsp.statusCode() == HttpResponseStatus.BAD_GATEWAY.code()) {
              doSend(endpoint, jsonData, url, host, times + 1);
              return Future.succeededFuture();
            }
            return rsp.body().compose(buffer -> {
              LOGGER.warn("Send data to url {} failed and status line is {}",
                  url,
                  rsp.statusCode());
              LOGGER.warn("message: {}", buffer);
              return Future.succeededFuture();
            });
          } else {
            EventManager.post(new MonitorSuccEvent());
          }
          return Future.succeededFuture();
        }).onFailure(failure -> {
          EventManager.post(new MonitorFailEvent("send monitor data fail."));
          LOGGER.warn("Send monitor data to {} failed , {}", endpoint, failure);
        });
      });
    });
  }

  private void deployMonitorClient() throws InterruptedException {
    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setAddressResolverOptions(AddressResolverConfig.getAddressResover(SSL_KEY));
    Vertx vertx = VertxUtils.getOrCreateVertxByName("monitor-center", vertxOptions);

    HttpClientOptions httpClientOptions = createHttpClientOptions();
    httpClientOptions.setMaxWaitQueueSize(MAX_WAIT_QUEUE_SIZE);
    clientMgr = new ClientPoolManager<>(vertx, new HttpClientPoolFactory(httpClientOptions));

    DeploymentOptions deploymentOptions = VertxUtils.createClientDeployOptions(clientMgr, 1);
    VertxUtils.blockDeploy(vertx, ClientVerticle.class, deploymentOptions);
  }

  private HttpClientOptions createHttpClientOptions() {
    HttpClientOptions httpClientOptions = new HttpClientOptions();
    if (MonitorConstant.isProxyEnable()) {
      ProxyOptions proxy = new ProxyOptions();
      proxy.setHost(MonitorConstant.getProxyHost());
      proxy.setPort(MonitorConstant.getProxyPort());
      proxy.setUsername(MonitorConstant.getProxyUsername());
      proxy.setPassword(MonitorConstant.getProxyPasswd());
      httpClientOptions.setProxyOptions(proxy);
    }

    httpClientOptions.setConnectTimeout(MonitorConstant.getConnectionTimeout());
    if (MonitorConstant.sslEnabled()) {
      SSLOptionFactory factory = SSLOptionFactory.createSSLOptionFactory(SSL_KEY, null);
      SSLOption sslOption;
      if (factory == null) {
        sslOption = SSLOption.buildFromYaml(SSL_KEY);
      } else {
        sslOption = factory.createSSLOption();
      }
      SSLCustom sslCustom = SSLCustom.createSSLCustom(sslOption.getSslCustomClass());
      VertxTLSBuilder.buildHttpClientOptions(sslOption, sslCustom, httpClientOptions);
    }
    return httpClientOptions;
  }
}
