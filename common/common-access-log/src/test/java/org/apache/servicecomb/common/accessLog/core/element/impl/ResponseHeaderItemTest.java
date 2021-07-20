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

import org.apache.servicecomb.core.event.InvocationFinishEvent;
import org.apache.servicecomb.core.event.ServerAccessLogEvent;
import org.apache.servicecomb.swagger.invocation.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.ext.web.RoutingContext;

public class ResponseHeaderItemTest {

  private static final String VAR_NAME = "varName";

  private static final ResponseHeaderAccessItem ELEMENT = new ResponseHeaderAccessItem(VAR_NAME);

  private StringBuilder strBuilder;

  private InvocationFinishEvent finishEvent;

  private ServerAccessLogEvent accessLogEvent;

  private RoutingContext routingContext;

  private HttpServerResponse serverResponse;

  private Response response;

  @Before
  public void initStrBuilder() {
    routingContext = Mockito.mock(RoutingContext.class);
    finishEvent = Mockito.mock(InvocationFinishEvent.class);
    serverResponse = Mockito.mock(HttpServerResponse.class);
    response = Mockito.mock(Response.class);

    accessLogEvent = new ServerAccessLogEvent();
    accessLogEvent.setRoutingContext(routingContext);
    strBuilder = new StringBuilder();
  }

  @Test
  public void serverFormattedElement() {
    HeadersMultiMap headers = new HeadersMultiMap();
    String headerValue = "headerValue";
    headers.add(VAR_NAME, headerValue);
    when(routingContext.response()).thenReturn(serverResponse);
    when(serverResponse.headers()).thenReturn(headers);

    ELEMENT.appendServerFormattedItem(accessLogEvent, strBuilder);
    assertEquals(headerValue, strBuilder.toString());
    assertEquals(ELEMENT.getVarName(), VAR_NAME);
  }

  @Test
  public void clientFormattedElement() {
    String headerValue = "headerValue";

    response = Response.ok(null)
        .setHeader(VAR_NAME, headerValue);
    when(finishEvent.getResponse()).thenReturn(response);

    ELEMENT.appendClientFormattedItem(finishEvent, strBuilder);
    assertEquals(headerValue, strBuilder.toString());
    assertEquals(ELEMENT.getVarName(), VAR_NAME);
  }

  @Test
  public void serverFormattedElementOnHeadersIsNull() {
    when(routingContext.response()).thenReturn(serverResponse);
    when(serverResponse.headers()).thenReturn(null);

    ELEMENT.appendServerFormattedItem(accessLogEvent, strBuilder);
    assertEquals("-", strBuilder.toString());
  }

  @Test
  public void clientFormattedElementOnHeadersIsNull() {
    when(finishEvent.getResponse()).thenReturn(response);
    when(response.getHeaders()).thenReturn(null);

    ELEMENT.appendClientFormattedItem(finishEvent, strBuilder);
    assertEquals("-", strBuilder.toString());
  }

  @Test
  public void serverFormattedElementOnResponseIsNull() {
    when(routingContext.response()).thenReturn(null);
    ELEMENT.appendServerFormattedItem(accessLogEvent, strBuilder);
    assertEquals("-", strBuilder.toString());
  }

  @Test
  public void clientFormattedElementOnResponseIsNull() {
    when(finishEvent.getResponse()).thenReturn(null);
    ELEMENT.appendClientFormattedItem(finishEvent, strBuilder);
    assertEquals("-", strBuilder.toString());
  }

  @Test
  public void serverFormattedElementOnNotFound() {
    HeadersMultiMap headers = new HeadersMultiMap();
    String headerValue = "headerValue";
    headers.add("anotherHeader", headerValue);
    when(routingContext.response()).thenReturn(serverResponse);
    when(serverResponse.headers()).thenReturn(headers);

    ELEMENT.appendServerFormattedItem(accessLogEvent, strBuilder);
    assertEquals("-", strBuilder.toString());
  }

  @Test
  public void clientFormattedElementOnNotFound() {
    String headerValue = "headerValue";
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.set("anotherHeader", headerValue);
    when(finishEvent.getResponse()).thenReturn(response);
    when(response.getHeaders()).thenReturn(headers);

    ELEMENT.appendClientFormattedItem(finishEvent, strBuilder);
    assertEquals("-", strBuilder.toString());
  }
}
