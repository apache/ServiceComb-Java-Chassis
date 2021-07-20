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

package org.apache.servicecomb.transport.rest.vertx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpHeaders;
import org.apache.servicecomb.common.rest.RestConst;
import org.apache.servicecomb.common.rest.RestProducerInvocation;
import org.apache.servicecomb.common.rest.VertxRestInvocation;
import org.apache.servicecomb.common.rest.filter.HttpServerFilter;
import org.apache.servicecomb.config.ConfigUtil;
import org.apache.servicecomb.core.SCBEngine;
import org.apache.servicecomb.core.Transport;
import org.apache.servicecomb.core.bootstrap.SCBBootstrap;
import org.apache.servicecomb.core.transport.TransportManager;
import org.apache.servicecomb.foundation.test.scaffolding.config.ArchaiusUtils;
import org.apache.servicecomb.foundation.vertx.http.HttpServletRequestEx;
import org.apache.servicecomb.foundation.vertx.http.HttpServletResponseEx;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;

public class TestVertxRestDispatcher {
  @Mocked
  Router mainRouter;

  @Mocked
  TransportManager transportManager;

  VertxRestDispatcher dispatcher;

  Throwable throwable;

  boolean invoked;

  @Before
  public void setUp() {
    ConfigUtil.installDynamicConfig();
    dispatcher = new VertxRestDispatcher();
    dispatcher.init(mainRouter);

    new MockUp<RestProducerInvocation>() {
      @Mock
      void sendFailResponse(Throwable throwable) {
        TestVertxRestDispatcher.this.throwable = throwable;
      }

      @Mock
      void invoke(Transport transport, HttpServletRequestEx requestEx, HttpServletResponseEx responseEx,
          List<HttpServerFilter> httpServerFilters) {
        invoked = true;
      }
    };

    SCBBootstrap.createSCBEngineForTest().setTransportManager(transportManager);
  }

  @After
  public void teardown() {
    SCBEngine.getInstance().destroy();
    ArchaiusUtils.resetConfig();
  }

  @Test
  public void getOrder() {
    Assert.assertEquals(Integer.MAX_VALUE, dispatcher.getOrder());
  }

  @Test
  public void failureHandlerNormal(@Mocked RoutingContext context) {
    RestProducerInvocation restProducerInvocation = new RestProducerInvocation();

    Exception e = new Exception();
    MockHttpServerResponse response = new MockHttpServerResponse();
    new Expectations() {
      {
        context.get(RestConst.REST_PRODUCER_INVOCATION);
        result = restProducerInvocation;
        context.failure();
        returns(e, e);
        context.response();
        result = response;
      }
    };

    Deencapsulation.invoke(dispatcher, "failureHandler", context);

    Assert.assertSame(e, this.throwable);
    Assert.assertTrue(response.responseClosed);
  }

  @Test
  public void failureHandlerErrorDataWithInvocation(@Mocked RoutingContext context, @Mocked InvocationException e) {
    RestProducerInvocation restProducerInvocation = new RestProducerInvocation();

    ErrorDataDecoderException edde = new ErrorDataDecoderException(e);
    MockHttpServerResponse response = new MockHttpServerResponse();
    new Expectations() {
      {
        context.get(RestConst.REST_PRODUCER_INVOCATION);
        result = restProducerInvocation;
        context.failure();
        returns(edde, edde);
        context.response();
        result = response;
      }
    };

    Deencapsulation.invoke(dispatcher, "failureHandler", context);

    Assert.assertSame(e, this.throwable);
    Assert.assertTrue(response.responseClosed);
  }

  @Test
  public void failureHandlerErrorDataWithNormal(@Mocked RoutingContext context) {
    RestProducerInvocation restProducerInvocation = new RestProducerInvocation();

    Exception e = new Exception();
    ErrorDataDecoderException edde = new ErrorDataDecoderException(e);
    MockHttpServerResponse response = new MockHttpServerResponse();
    new Expectations() {
      {
        context.get(RestConst.REST_PRODUCER_INVOCATION);
        result = restProducerInvocation;
        context.failure();
        returns(edde, edde);
        context.response();
        result = response;
      }
    };

    Deencapsulation.invoke(dispatcher, "failureHandler", context);

    Assert.assertSame(edde, this.throwable);
    Assert.assertTrue(response.responseClosed);
  }

  @Test
  public void failureHandlerWithNoRestProducerInvocationAndInvocationException(@Mocked RoutingContext context) {
    InvocationException e = new InvocationException(Status.REQUEST_ENTITY_TOO_LARGE, "testMsg");
    ErrorDataDecoderException edde = new ErrorDataDecoderException(e);
    MockHttpServerResponse response = new MockHttpServerResponse();
    new Expectations() {
      {
        context.get(RestConst.REST_PRODUCER_INVOCATION);
        result = null;
        context.failure();
        returns(edde, edde);
        context.response();
        result = response;
      }
    };

    Deencapsulation.invoke(dispatcher, "failureHandler", context);

    Assert.assertThat(response.responseHeader, Matchers.hasEntry(HttpHeaders.CONTENT_TYPE, MediaType.WILDCARD));
    Assert.assertThat(response.responseStatusCode, Matchers.is(Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()));
    Assert.assertThat(response.responseStatusMessage, Matchers.is(Status.REQUEST_ENTITY_TOO_LARGE.getReasonPhrase()));
    Assert.assertThat(response.responseChunk,
        Matchers.is("{\"message\":\"" + Status.REQUEST_ENTITY_TOO_LARGE.getReasonPhrase() + "\"}"));
    Assert.assertTrue(response.responseEnded);
  }

  @Test
  public void failureHandlerWithNoRestProducerInvocationAndOtherException(@Mocked RoutingContext context) {
    String exceptionMessage = "test exception message";
    Exception exception = new Exception(exceptionMessage);
    MockHttpServerResponse response = new MockHttpServerResponse();
    new Expectations() {
      {
        context.get(RestConst.REST_PRODUCER_INVOCATION);
        result = null;
        context.failure();
        returns(exception, exception);
        context.response();
        result = response;
      }
    };

    Deencapsulation.invoke(dispatcher, "failureHandler", context);

    Assert.assertThat(response.responseHeader, Matchers.hasEntry(HttpHeaders.CONTENT_TYPE, MediaType.WILDCARD));
    Assert.assertThat(response.responseStatusCode, Matchers.is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    Assert.assertThat(response.responseChunk,
        Matchers.is("{\"message\":\"" + exceptionMessage + "\"}"));
    Assert.assertTrue(response.responseEnded);
  }

  @Test
  public void failureHandlerWithNoExceptionAndStatusCodeIsSet(@Mocked RoutingContext context) {
    MockHttpServerResponse response = new MockHttpServerResponse();
    new Expectations() {
      {
        context.get(RestConst.REST_PRODUCER_INVOCATION);
        result = null;
        context.failure();
        returns(null, null);
        context.response();
        result = response;
        context.statusCode();
        result = Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode();
      }
    };

    Deencapsulation.invoke(dispatcher, "failureHandler", context);

    Assert.assertThat(response.responseHeader, Matchers.hasEntry(HttpHeaders.CONTENT_TYPE, MediaType.WILDCARD));
    Assert.assertThat(response.responseStatusCode, Matchers.is(Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()));
    Assert.assertTrue(response.responseEnded);
  }

  @Test
  public void failureHandlerWithNoExceptionAndStatusCodeIsNotSet(@Mocked RoutingContext context) {
    MockHttpServerResponse response = new MockHttpServerResponse();
    new Expectations() {
      {
        context.get(RestConst.REST_PRODUCER_INVOCATION);
        result = null;
        context.failure();
        returns(null, null);
        context.response();
        result = response;
        context.statusCode();
        result = Status.OK.getStatusCode();
      }
    };

    Deencapsulation.invoke(dispatcher, "failureHandler", context);

    Assert.assertThat(response.responseHeader, Matchers.hasEntry(HttpHeaders.CONTENT_TYPE, MediaType.WILDCARD));
    Assert.assertThat(response.responseStatusCode, Matchers.is(Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    Assert.assertThat(response.responseStatusMessage, Matchers.is(Status.INTERNAL_SERVER_ERROR.getReasonPhrase()));
    Assert.assertThat(response.responseChunk,
        Matchers.is("{\"message\":\"" + Status.INTERNAL_SERVER_ERROR.getReasonPhrase() + "\"}"));
    Assert.assertTrue(response.responseEnded);
  }

  @Test
  public void onRequest(@Mocked Vertx vertx, @Mocked Context context, @Mocked HttpServerRequest request,
      @Mocked SocketAddress socketAdrress) {
    Map<String, Object> map = new HashMap<>();
    RoutingContext routingContext = new MockUp<RoutingContext>() {
      @Mock
      RoutingContext put(String key, Object obj) {
        map.put(key, obj);
        return null;
      }

      @Mock
      HttpServerRequest request() {
        return request;
      }
    }.getMockInstance();

    new Expectations() {
      {
        Vertx.currentContext();
        result = context;
      }
    };

    Deencapsulation.invoke(dispatcher, "onRequest", routingContext);

    Assert.assertEquals(VertxRestInvocation.class, map.get(RestConst.REST_PRODUCER_INVOCATION).getClass());
    Assert.assertTrue(invoked);
  }

  @Test
  public void testWrapResponseBody() {
    VertxRestDispatcher vertxRestDispatcher = new VertxRestDispatcher();
    String message = "abcd";
    String bodyString = vertxRestDispatcher.wrapResponseBody(message);
    Assert.assertNotNull(bodyString);
    Assert.assertEquals("{\"message\":\"abcd\"}", bodyString);

    message = "\"abcd\"";
    bodyString = vertxRestDispatcher.wrapResponseBody(message);
    Assert.assertNotNull(bodyString);
    Assert.assertEquals("{\"message\":\"\\\"abcd\\\"\"}", bodyString);

    message = ".01ab\"!@#$%^&*()'\\cd";
    bodyString = vertxRestDispatcher.wrapResponseBody(message);
    Assert.assertNotNull(bodyString);
    Assert.assertEquals("{\"message\":\".01ab\\\"!@#$%^&*()'\\\\cd\"}", bodyString);

    message = new JsonObject().put("key", new JsonObject().put("k2", "value")).toString();
    bodyString = vertxRestDispatcher.wrapResponseBody(message);
    Assert.assertNotNull(bodyString);
    Assert.assertEquals("{\"key\":{\"k2\":\"value\"}}", bodyString);

    message = "ab\"23\n@!#cd";
    bodyString = vertxRestDispatcher.wrapResponseBody(message);
    Assert.assertNotNull(bodyString);
    Assert.assertEquals("{\"message\":\"ab\\\"23\\n@!#cd\"}", bodyString);

    message = "ab\"23\r\n@!#cd";
    bodyString = vertxRestDispatcher.wrapResponseBody(message);
    Assert.assertNotNull(bodyString);
    Assert.assertEquals("{\"message\":\"ab\\\"23\\r\\n@!#cd\"}", bodyString);
  }
}

class MockHttpServerResponse implements HttpServerResponse {
  boolean responseClosed;

  boolean responseEnded;

  Map<String, String> responseHeader = new HashMap<>(1);

  int responseStatusCode;

  String responseStatusMessage;

  String responseChunk;

  @Override
  public void close() {
    responseClosed = true;
  }

  @Override
  public HttpServerResponse putHeader(String name, String value) {
    responseHeader.put(name, value);
    return this;
  }

  @Override
  public HttpServerResponse setStatusCode(int statusCode) {
    responseStatusCode = statusCode;
    return this;
  }

  @Override
  public HttpServerResponse setStatusMessage(String statusMessage) {
    responseStatusMessage = statusMessage;
    return this;
  }

  @Override
  public Future<Void> end() {
    responseEnded = true;
    return Future.succeededFuture();
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public Future<Void> end(String chunk) {
    responseEnded = true;
    responseChunk = chunk;
    return Future.succeededFuture();
  }

  @Override
  public void end(String s, Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
    return null;
  }

  @Override
  public Future<Void> write(Buffer data) {
    return Future.succeededFuture();
  }

  @Override
  public void write(Buffer buffer, Handler<AsyncResult<Void>> handler) {
  }

  @Override
  public HttpServerResponse setWriteQueueMaxSize(int maxSize) {
    return null;
  }

  @Override
  public boolean writeQueueFull() {
    return false;
  }

  @Override
  public HttpServerResponse drainHandler(Handler<Void> handler) {
    return null;
  }

  @Override
  public int getStatusCode() {
    return 0;
  }

  @Override
  public String getStatusMessage() {
    return null;
  }

  @Override
  public HttpServerResponse setChunked(boolean chunked) {
    return null;
  }

  @Override
  public boolean isChunked() {
    return false;
  }

  @Override
  public MultiMap headers() {
    return null;
  }

  @Override
  public HttpServerResponse putHeader(CharSequence name, CharSequence value) {
    return null;
  }

  @Override
  public HttpServerResponse putHeader(String name, Iterable<String> values) {
    return null;
  }

  @Override
  public HttpServerResponse putHeader(CharSequence name, Iterable<CharSequence> values) {
    return null;
  }

  @Override
  public MultiMap trailers() {
    return null;
  }

  @Override
  public HttpServerResponse putTrailer(String name, String value) {
    return null;
  }

  @Override
  public HttpServerResponse putTrailer(CharSequence name, CharSequence value) {
    return null;
  }

  @Override
  public HttpServerResponse putTrailer(String name, Iterable<String> values) {
    return null;
  }

  @Override
  public HttpServerResponse putTrailer(CharSequence name, Iterable<CharSequence> value) {
    return null;
  }

  @Override
  public HttpServerResponse closeHandler(Handler<Void> handler) {
    return null;
  }

  @Override
  public HttpServerResponse endHandler(Handler<Void> handler) {
    return null;
  }

  @Override
  public Future<Void> write(String chunk, String enc) {
    return Future.succeededFuture();
  }

  @Override
  public void write(String s, String s1, Handler<AsyncResult<Void>> handler) {
  }

  @Override
  public Future<Void> write(String chunk) {
    return Future.succeededFuture();
  }

  @Override
  public void write(String s, Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public HttpServerResponse writeContinue() {
    return null;
  }

  @Override
  public Future<Void> end(String chunk, String enc) {
    return Future.succeededFuture();
  }

  @Override
  public void end(String s, String s1, Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public Future<Void> end(Buffer chunk) {
    return Future.succeededFuture();
  }

  @Override
  public void end(Buffer buffer, Handler<AsyncResult<Void>> handler) {

  }

  @Override
  public Future<Void> sendFile(String filename, long offset, long length) {
    return Future.succeededFuture();
  }

  @Override
  public HttpServerResponse sendFile(String filename, long offset, long length,
      Handler<AsyncResult<Void>> resultHandler) {
    return null;
  }

  @Override
  public boolean ended() {
    return false;
  }

  @Override
  public boolean closed() {
    return false;
  }

  @Override
  public boolean headWritten() {
    return false;
  }

  @Override
  public HttpServerResponse headersEndHandler(Handler<Void> handler) {
    return null;
  }

  @Override
  public HttpServerResponse bodyEndHandler(Handler<Void> handler) {
    return null;
  }

  @Override
  public long bytesWritten() {
    return 0;
  }

  @Override
  public int streamId() {
    return 0;
  }

  @Override
  public HttpServerResponse push(HttpMethod method, String host, String path,
      Handler<AsyncResult<HttpServerResponse>> handler) {
    return null;
  }

  @Override
  public HttpServerResponse push(HttpMethod method, String path, MultiMap headers,
      Handler<AsyncResult<HttpServerResponse>> handler) {
    return null;
  }

  @Override
  public HttpServerResponse push(HttpMethod method, String path, Handler<AsyncResult<HttpServerResponse>> handler) {
    return null;
  }

  @Override
  public HttpServerResponse push(HttpMethod method, String host, String path, MultiMap headers,
      Handler<AsyncResult<HttpServerResponse>> handler) {
    return null;
  }

  @Override
  public Future<HttpServerResponse> push(HttpMethod method, String host, String path, MultiMap headers) {
    return Future.succeededFuture();
  }

  @Override
  public boolean reset(long code) {
    return false;
  }

  @Override
  public HttpServerResponse writeCustomFrame(int type, int flags, Buffer payload) {
    return null;
  }

  @Override
  public HttpServerResponse addCookie(Cookie cookie) {
    return null;
  }

  @Override
  public @Nullable Cookie removeCookie(String s, boolean b) {
    return null;
  }
}
