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

package org.apache.servicecomb.common.accessLog.core.element.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.servicecomb.common.rest.RestConst;
import org.apache.servicecomb.common.rest.codec.param.RestClientRequestImpl;
import org.apache.servicecomb.core.Endpoint;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.core.event.InvocationFinishEvent;
import org.apache.servicecomb.core.event.ServerAccessLogEvent;
import org.apache.servicecomb.foundation.common.net.URIEndpointObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.RoutingContext;

public class FirstLineOfRequestItemTest {

  public static final FirstLineOfRequestAccessItem ELEMENT = new FirstLineOfRequestAccessItem();

  private StringBuilder strBuilder;

  private InvocationFinishEvent finishEvent;

  private ServerAccessLogEvent accessLogEvent;

  private RoutingContext mockContext;

  private Invocation invocation;

  private RestClientRequestImpl restClientRequest;

  private HttpClientRequest clientRequest;

  private Endpoint endpoint;

  private URIEndpointObject urlEndpoint;

  @Before
  public void initStrBuilder() {
    mockContext = Mockito.mock(RoutingContext.class);
    finishEvent = Mockito.mock(InvocationFinishEvent.class);
    invocation = Mockito.mock(Invocation.class);
    restClientRequest = Mockito.mock(RestClientRequestImpl.class);
    clientRequest = Mockito.mock(HttpClientRequest.class);
    endpoint = Mockito.mock(Endpoint.class);
    urlEndpoint = Mockito.mock(URIEndpointObject.class);
    Map<String, Object> handlerMap = new HashMap<>();
    handlerMap.put(RestConst.INVOCATION_HANDLER_REQUESTCLIENT, restClientRequest);
    when(finishEvent.getInvocation()).thenReturn(invocation);
    when(invocation.getHandlerContext()).thenReturn(handlerMap);
    when(restClientRequest.getRequest()).thenReturn(clientRequest);
    when(invocation.getEndpoint()).thenReturn(endpoint);
    when(endpoint.getAddress()).thenReturn(urlEndpoint);
    accessLogEvent = new ServerAccessLogEvent();
    accessLogEvent.setRoutingContext(mockContext);
    strBuilder = new StringBuilder();
  }

  @Test
  public void serverFormattedElement() {
    HttpServerRequest request = Mockito.mock(HttpServerRequest.class);
    String uri = "/test/uri";

    when(mockContext.request()).thenReturn(request);
    when(request.method()).thenReturn(HttpMethod.DELETE);
    when(request.path()).thenReturn(uri);
    when(request.version()).thenReturn(HttpVersion.HTTP_1_1);

    ELEMENT.appendServerFormattedItem(accessLogEvent, strBuilder);
    assertEquals("\"DELETE " + uri + " HTTP/1.1\"", strBuilder.toString());
  }

  @Test
  public void clientFormattedElement() {
    String uri = "/test/uri";
    when(clientRequest.getMethod()).thenReturn(HttpMethod.DELETE);
    when(clientRequest.path()).thenReturn(uri);
    when(urlEndpoint.isHttp2Enabled()).thenReturn(true);

    ELEMENT.appendClientFormattedItem(finishEvent, strBuilder);
    assertEquals("\"DELETE " + uri + " HTTP/2.0\"", strBuilder.toString());
  }
}
