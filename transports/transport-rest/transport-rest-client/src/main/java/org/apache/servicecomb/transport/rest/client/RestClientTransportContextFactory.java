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
package org.apache.servicecomb.transport.rest.client;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.apache.servicecomb.transport.rest.client.RestClientExceptionCodes.FAILED_TO_CREATE_REST_CLIENT_TRANSPORT_CONTEXT;

import org.apache.servicecomb.common.rest.RestConst;
import org.apache.servicecomb.common.rest.definition.RestMetaUtils;
import org.apache.servicecomb.common.rest.definition.RestOperationMeta;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.foundation.common.net.URIEndpointObject;
import org.apache.servicecomb.foundation.vertx.client.http.HttpClientWithContext;
import org.apache.servicecomb.foundation.vertx.client.http.HttpClients;
import org.apache.servicecomb.registry.definition.DefinitionConst;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

@Component
public class RestClientTransportContextFactory {
  private BoundaryFactory boundaryFactory = BoundaryFactory.DEFAULT;

  private HttpClientRequestFactory httpClientRequestFactory = HttpClientRequestFactory.DEFAULT;

  @Autowired(required = false)
  public RestClientTransportContextFactory setBoundaryFactory(BoundaryFactory boundaryFactory) {
    this.boundaryFactory = boundaryFactory;
    return this;
  }

  @Autowired(required = false)
  public RestClientTransportContextFactory setHttpClientRequestFactory(HttpClientRequestFactory factory) {
    this.httpClientRequestFactory = factory;
    return this;
  }

  public RestClientTransportContext create(Invocation invocation, HttpClientRequest httpClientRequest) {
    try {
      return doCreate(invocation, httpClientRequest);
    } catch (Throwable e) {
      throw new InvocationException(BAD_REQUEST, FAILED_TO_CREATE_REST_CLIENT_TRANSPORT_CONTEXT, e.getMessage(), e);
    }
  }

  protected RestClientTransportContext doCreate(Invocation invocation, HttpClientRequest httpClientRequest)
      throws Throwable {
    RestOperationMeta restOperationMeta = RestMetaUtils.getRestOperationMeta(invocation.getOperationMeta());
    HttpClientWithContext httpClientWithContext = findHttpClientPool(invocation);
    return new RestClientTransportContext(restOperationMeta,
        httpClientWithContext.context(),
        httpClientRequest,
        boundaryFactory);
  }

  protected HttpClientWithContext findHttpClientPool(Invocation invocation) {
    URIEndpointObject endpoint = (URIEndpointObject) invocation.getEndpoint().getAddress();
    if (endpoint.isHttp2Enabled()) {
      return HttpClients.getClient(Http2TransportHttpClientOptionsSPI.CLIENT_NAME, invocation.isSync());
    }

    return HttpClients.getClient(HttpTransportHttpClientOptionsSPI.CLIENT_NAME, invocation.isSync());
  }

  protected Future<HttpClientRequest> createHttpClientRequest(Invocation invocation) {
    try {
      RestOperationMeta restOperationMeta = RestMetaUtils.getRestOperationMeta(invocation.getOperationMeta());
      HttpClientWithContext httpClientWithContext = findHttpClientPool(invocation);

      URIEndpointObject endpoint = (URIEndpointObject) invocation.getEndpoint().getAddress();
      HttpMethod method = HttpMethod.valueOf(restOperationMeta.getHttpMethod());
      RequestOptions requestOptions = new RequestOptions()
          .setHost(endpoint.getHostOrIp())
          .setPort(endpoint.getPort())
          .setSsl(endpoint.isSslEnabled())
          .setMethod(method)
          .setURI(createRequestPath(invocation, restOperationMeta));
      return httpClientRequestFactory.create(invocation, httpClientWithContext.getHttpClient(), requestOptions);
    } catch (Throwable e) {
      throw new InvocationException(BAD_REQUEST, FAILED_TO_CREATE_REST_CLIENT_TRANSPORT_CONTEXT, e.getMessage(), e);
    }
  }

  protected String createRequestPath(Invocation invocation, RestOperationMeta restOperationMeta) throws Exception {
    String path = invocation.getLocalContext(RestConst.REST_CLIENT_REQUEST_PATH);
    if (path == null) {
      path = restOperationMeta.getPathBuilder().createRequestPath(invocation.getSwaggerArguments());
    }

    URIEndpointObject endpoint = (URIEndpointObject) invocation.getEndpoint().getAddress();
    String urlPrefix = endpoint.getFirst(DefinitionConst.URL_PREFIX);
    if (StringUtils.isEmpty(urlPrefix) || path.startsWith(urlPrefix)) {
      return path;
    }

    return urlPrefix + path;
  }
}
