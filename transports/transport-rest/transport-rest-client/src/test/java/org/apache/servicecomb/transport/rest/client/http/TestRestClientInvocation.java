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

package org.apache.servicecomb.transport.rest.client.http;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.servicecomb.common.rest.RestConst;
import org.apache.servicecomb.common.rest.definition.RestOperationMeta;
import org.apache.servicecomb.common.rest.definition.path.URLPathBuilder;
import org.apache.servicecomb.common.rest.filter.HttpClientFilter;
import org.apache.servicecomb.core.Endpoint;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.core.definition.OperationConfig;
import org.apache.servicecomb.core.definition.OperationMeta;
import org.apache.servicecomb.core.executor.ReactiveExecutor;
import org.apache.servicecomb.core.invocation.InvocationStageTrace;
import org.apache.servicecomb.core.tracing.TraceIdLogger;
import org.apache.servicecomb.foundation.common.net.URIEndpointObject;
import org.apache.servicecomb.foundation.common.utils.JsonUtils;
import org.apache.servicecomb.foundation.test.scaffolding.exception.RuntimeExceptionWithoutStackTrace;
import org.apache.servicecomb.foundation.test.scaffolding.log.LogCollector;
import org.apache.servicecomb.foundation.vertx.client.http.HttpClientWithContext;
import org.apache.servicecomb.registry.definition.DefinitionConst;
import org.apache.servicecomb.swagger.invocation.AsyncResponse;
import org.apache.servicecomb.swagger.invocation.Response;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;

public class TestRestClientInvocation {

  private static final String TARGET_MICROSERVICE_NAME = "TargetMS";

  Handler<Buffer> bodyHandler;

  MultiMap headers = new HeadersMultiMap();

  HttpClientRequest request = mock(HttpClientRequest.class);

  HttpClientResponse httpClientResponse = mock(HttpClientResponse.class);

  HttpClient httpClient = mock(HttpClient.class);

  Context context = mock(Context.class);

  HttpClientWithContext httpClientWithContext = new HttpClientWithContext(httpClient, context);

  Invocation invocation = mock(Invocation.class);

  InvocationStageTrace invocationStageTrace = new InvocationStageTrace(invocation);

  Response response;

  AsyncResponse asyncResp = resp -> response = resp;

  OperationMeta operationMeta = mock(OperationMeta.class);

  Endpoint endpoint = mock(Endpoint.class);

  RestOperationMeta swaggerRestOperation = mock(RestOperationMeta.class);

  URLPathBuilder urlPathBuilder = mock(URLPathBuilder.class);

  URIEndpointObject address = mock(URIEndpointObject.class);

  List<HttpClientFilter> httpClientFilters = new ArrayList<>();

  RestClientInvocation restClientInvocation = new RestClientInvocation(httpClientWithContext, httpClientFilters);

  Map<String, Object> handlerContext = new HashMap<>();

  static long nanoTime = 123;

  OperationConfig operationConfig = new OperationConfig();

  @BeforeClass
  public static void classSetup() {
    new MockUp<System>() {
      @Mock
      long nanoTime() {
        return nanoTime;
      }
    };
  }

  @SuppressWarnings({"unchecked"})
  @Before
  public void setup() {
    Deencapsulation.setField(restClientInvocation, "clientRequest", request);
    Deencapsulation.setField(restClientInvocation, "invocation", invocation);
    Deencapsulation.setField(restClientInvocation, "asyncResp", asyncResp);

    when(invocation.getMicroserviceName()).thenReturn(TARGET_MICROSERVICE_NAME);
    when(invocation.getOperationMeta()).thenReturn(operationMeta);
    when(swaggerRestOperation.getPathBuilder()).thenReturn(urlPathBuilder);
    when(swaggerRestOperation.getHttpMethod()).thenReturn("GET");
    when(operationMeta.getExtData(RestConst.SWAGGER_REST_OPERATION)).thenReturn(swaggerRestOperation);
    when(operationMeta.getConfig()).thenReturn(operationConfig);
    when(invocation.getEndpoint()).thenReturn(endpoint);
    when(invocation.getTraceIdLogger()).thenReturn(new TraceIdLogger(invocation));
    when(endpoint.getAddress()).thenReturn(address);
    when(invocation.getHandlerContext()).then(answer -> handlerContext);
    when(invocation.getInvocationStageTrace()).thenReturn(invocationStageTrace);
    when(httpClient.request(Mockito.any()))
        .thenReturn(Future.succeededFuture(request));
    when(request.headers()).thenReturn(headers);
    when(request.response()).thenReturn(Future.succeededFuture(httpClientResponse));
    doAnswer(a -> {
      headers.add(a.getArgumentAt(0, String.class), a.getArgumentAt(1, String.class));
      return request;
    }).when(request).putHeader(any(), (String) any());
    doAnswer(a -> {
      ((Handler<Void>) a.getArguments()[0]).handle(null);
      return null;
    }).when(context).runOnContext(any());
  }

  @Test
  public void invoke(@Mocked Response resp) throws Exception {
    doAnswer(a -> {
      asyncResp.complete(resp);
      return null;
    }).when(request).end();
    when(request.send()).thenReturn(Future.succeededFuture(mock(HttpClientResponse.class)));
    restClientInvocation.invoke(invocation, asyncResp);

    Assert.assertSame(resp, response);
    Assert.assertThat(headers.names(),
        Matchers.containsInAnyOrder(org.apache.servicecomb.core.Const.TARGET_MICROSERVICE,
            org.apache.servicecomb.core.Const.CSE_CONTEXT));
    Assert.assertEquals(TARGET_MICROSERVICE_NAME, headers.get(org.apache.servicecomb.core.Const.TARGET_MICROSERVICE));
    Assert.assertEquals("{}", headers.get(org.apache.servicecomb.core.Const.CSE_CONTEXT));
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getStartClientFiltersRequest());
  }

  @Test
  public void invoke_3rdPartyService(@Mocked Response resp) throws Exception {
    doAnswer(a -> {
      asyncResp.complete(resp);
      return null;
    }).when(request).end();
    when(invocation.isThirdPartyInvocation()).thenReturn(true);
    when(request.send()).thenReturn(Future.succeededFuture(mock(HttpClientResponse.class)));

    restClientInvocation.invoke(invocation, asyncResp);

    Assert.assertSame(resp, response);
    Assert.assertThat(headers.names(), Matchers.empty());
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getStartClientFiltersRequest());
  }

  @Test
  public void invoke_3rdPartyServiceExposeServiceCombHeaders(@Mocked Response resp) throws Exception {
    doAnswer(a -> {
      asyncResp.complete(resp);
      return null;
    }).when(request).end();
    when(request.send()).thenReturn(Future.succeededFuture(mock(HttpClientResponse.class)));
    when(invocation.isThirdPartyInvocation()).thenReturn(true);
    operationConfig.setClientRequestHeaderFilterEnabled(false);

    restClientInvocation.invoke(invocation, asyncResp);

    Assert.assertSame(resp, response);
    Assert.assertThat(headers.names(),
        Matchers.containsInAnyOrder(org.apache.servicecomb.core.Const.TARGET_MICROSERVICE,
            org.apache.servicecomb.core.Const.CSE_CONTEXT));
    Assert.assertEquals(TARGET_MICROSERVICE_NAME, headers.get(org.apache.servicecomb.core.Const.TARGET_MICROSERVICE));
    Assert.assertEquals("{}", headers.get(org.apache.servicecomb.core.Const.CSE_CONTEXT));
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getStartClientFiltersRequest());
    operationConfig.setClientRequestHeaderFilterEnabled(true);
  }

  @Test
  public void invoke_endThrow() throws Exception {
    Mockito.doThrow(Error.class).when(request).end();
    when(request.send()).thenReturn(Future.succeededFuture(mock(HttpClientResponse.class)));
    restClientInvocation.invoke(invocation, asyncResp);

    Assert.assertThat(((InvocationException) response.getResult()).getCause(), Matchers.instanceOf(Error.class));
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getStartClientFiltersRequest());
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getFinishClientFiltersResponse());
  }

  @Test
  public void invoke_requestThrow() throws Exception {
    Throwable t = new RuntimeExceptionWithoutStackTrace();
    doAnswer(a -> {
      throw t;
    }).when(request).end();
    when(request.send()).thenReturn(Future.succeededFuture(mock(HttpClientResponse.class)));
    restClientInvocation.invoke(invocation, asyncResp);
    restClientInvocation.invoke(invocation, asyncResp);

    Assert.assertThat(((InvocationException) response.getResult()).getCause(), Matchers.sameInstance(t));
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getStartClientFiltersRequest());
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getFinishClientFiltersResponse());
  }

  @Test
  public void testSetCseContext() {
    Map<String, String> contextMap = Collections.singletonMap("k", "v");
    when(invocation.getContext()).thenReturn(contextMap);

    restClientInvocation.setCseContext();

    Assert.assertEquals("x-cse-context={\"k\":\"v\"}\n", headers.toString());
  }

  @Test
  public void testSetCseContext_failed() throws JsonProcessingException {
    LogCollector logCollector = new LogCollector();
    logCollector.setLogLevel(RestClientInvocation.class.getName(), Level.DEBUG);

    new Expectations(JsonUtils.class) {
      {
        JsonUtils.writeValueAsString(any);
        result = new RuntimeExceptionWithoutStackTrace();
      }
    };

    restClientInvocation.setCseContext();

    Assert.assertEquals(
        "Failed to encode and set cseContext, message=cause:RuntimeExceptionWithoutStackTrace,message:null.",
        logCollector.getEvents().get(0).getMessage());
    logCollector.teardown();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void handleResponse() {
    HttpClientResponse httpClientResponse = mock(HttpClientResponse.class);
    doAnswer(a -> {
      bodyHandler = (Handler<Buffer>) a.getArguments()[0];
      return httpClientResponse;
    }).when(httpClientResponse).bodyHandler(any());

    Buffer buf = Buffer.buffer();
    new MockUp<RestClientInvocation>(restClientInvocation) {
      @Mock
      void processResponseBody(Buffer responseBuf) {
        asyncResp.success(buf);
      }
    };

    restClientInvocation.handleResponse(httpClientResponse);
    bodyHandler.handle(buf);

    Assert.assertSame(buf, response.getResult());
  }

  @Test
  public void processResponseBody() {
    Response resp = Response.ok(null);

    HttpClientResponse httpClientResponse = mock(HttpClientResponse.class);
    Deencapsulation.setField(restClientInvocation, "clientResponse", httpClientResponse);

    {
      HttpClientFilter filter = mock(HttpClientFilter.class);
      when(filter.afterReceiveResponse(any(), any())).thenReturn(null);
      when(filter.enabled()).thenReturn(true);
      httpClientFilters.add(filter);
    }
    {
      HttpClientFilter filter = mock(HttpClientFilter.class);
      when(filter.afterReceiveResponse(any(), any())).thenReturn(resp);
      when(filter.enabled()).thenReturn(true);
      httpClientFilters.add(filter);
    }

    when(invocation.getResponseExecutor()).thenReturn(new ReactiveExecutor());

    restClientInvocation.processResponseBody(null);

    Assert.assertSame(resp, response);
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getStartClientFiltersResponse());
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getFinishClientFiltersResponse());
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getFinishReceiveResponse());
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getFinishWriteToBuffer());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void processResponseBody_throw() {
    HttpClientResponse httpClientResponse = mock(HttpClientResponse.class);
    Deencapsulation.setField(restClientInvocation, "clientResponse", httpClientResponse);

    {
      HttpClientFilter filter = mock(HttpClientFilter.class);
      when(filter.afterReceiveResponse(any(), any())).thenThrow(Error.class);
      when(filter.enabled()).thenReturn(true);
      httpClientFilters.add(filter);
    }

    when(invocation.getResponseExecutor()).thenReturn(new ReactiveExecutor());
    restClientInvocation.processResponseBody(null);

    Assert.assertThat(((InvocationException) response.getResult()).getCause(), Matchers.instanceOf(Error.class));
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getStartClientFiltersResponse());
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getFinishClientFiltersResponse());
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getFinishReceiveResponse());
    Assert.assertEquals(nanoTime, invocation.getInvocationStageTrace().getFinishWriteToBuffer());
  }

  @Test
  public void createRequestPath_NoUrlPrefixNoPath() throws Exception {
    when(address.getFirst(DefinitionConst.URL_PREFIX)).thenReturn(null);

    when(urlPathBuilder.createRequestPath(any())).thenReturn("/path");

    String path = restClientInvocation.createRequestPath(swaggerRestOperation);
    Assert.assertEquals("/path", path);
  }

  @Test
  public void createRequestPath_noUrlPrefixHavePath() throws Exception {
    handlerContext.put(RestConst.REST_CLIENT_REQUEST_PATH, "/client/path");
    when(address.getFirst(DefinitionConst.URL_PREFIX)).thenReturn(null);

    String path = restClientInvocation.createRequestPath(swaggerRestOperation);
    Assert.assertEquals("/client/path", path);
  }

  @Test
  public void createRequestPath_haveUrlPrefixNoPath() throws Exception {
    when(address.getFirst(DefinitionConst.URL_PREFIX)).thenReturn("/prefix");

    when(urlPathBuilder.createRequestPath(any())).thenReturn("/path");

    String path = restClientInvocation.createRequestPath(swaggerRestOperation);
    Assert.assertEquals("/prefix/path", path);
  }

  @Test
  public void createRequestPath_haveUrlPrefixHavePath() throws Exception {
    when(address.getFirst(DefinitionConst.URL_PREFIX)).thenReturn("/prefix");
    handlerContext.put(RestConst.REST_CLIENT_REQUEST_PATH, "/client/path");

    String path = restClientInvocation.createRequestPath(swaggerRestOperation);
    Assert.assertEquals("/prefix/client/path", path);
  }

  @Test
  public void createRequestPath_haveUrlPrefixHavePathAndStartWith() throws Exception {
    when(address.getFirst(DefinitionConst.URL_PREFIX)).thenReturn("/prefix");
    handlerContext.put(RestConst.REST_CLIENT_REQUEST_PATH, "/prefix/client/path");

    String path = restClientInvocation.createRequestPath(swaggerRestOperation);
    Assert.assertEquals("/prefix/client/path", path);
  }
}
