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

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.servicecomb.common.rest.RestConst;
import org.apache.servicecomb.common.rest.codec.param.RestClientRequestImpl;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.core.event.InvocationFinishEvent;
import org.apache.servicecomb.core.event.ServerAccessLogEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.vertx.core.http.Cookie;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.ext.web.RoutingContext;

public class CookieItemTest {

  public static final String COOKIE_NAME = "cookieName";

  public static final String COOKIE_VALUE = "cookieVALUE";

  private static final CookieAccessItem ELEMENT = new CookieAccessItem(COOKIE_NAME);

  private StringBuilder strBuilder;

  private InvocationFinishEvent finishEvent;

  private ServerAccessLogEvent accessLogEvent;

  private RoutingContext mockContext;

  private Invocation invocation;

  private RestClientRequestImpl restClientRequest;

  @Before
  public void initStrBuilder() {
    mockContext = Mockito.mock(RoutingContext.class);
    finishEvent = Mockito.mock(InvocationFinishEvent.class);
    invocation = Mockito.mock(Invocation.class);
    restClientRequest = Mockito.mock(RestClientRequestImpl.class);

    accessLogEvent = new ServerAccessLogEvent();
    strBuilder = new StringBuilder();
  }

  @Test
  public void serverFormattedElement() {
    HashMap<String, Cookie> cookieSet = new HashMap<>();
    CookieImpl cookie = new CookieImpl(COOKIE_NAME, COOKIE_VALUE);

    cookieSet.put(cookie.getName(), cookie);
    when(mockContext.cookieCount()).thenReturn(1);
    when(mockContext.cookieMap()).thenReturn(cookieSet);
    accessLogEvent.setRoutingContext(mockContext);

    ELEMENT.appendServerFormattedItem(accessLogEvent, strBuilder);
    Assert.assertEquals(COOKIE_VALUE, strBuilder.toString());
  }

  @Test
  public void clientFormattedElement() {
    Map<String, Object> handlerMap = new HashMap<>();
    handlerMap.put(RestConst.INVOCATION_HANDLER_REQUESTCLIENT, restClientRequest);

    Map<String, String> cookieMap = new HashMap<>();
    cookieMap.put(COOKIE_NAME, COOKIE_VALUE);

    when(finishEvent.getInvocation()).thenReturn(invocation);
    when(invocation.getHandlerContext()).thenReturn(handlerMap);
    when(restClientRequest.getCookieMap()).thenReturn(cookieMap);

    ELEMENT.appendClientFormattedItem(finishEvent, strBuilder);
    Assert.assertEquals(COOKIE_VALUE, strBuilder.toString());
  }

  @Test
  public void serverFormattedElementOnCookieCountIsZero() {
    HashMap<String, Cookie> cookieSet = new HashMap<>();
    Mockito.when(mockContext.cookieCount()).thenReturn(0);
    Mockito.when(mockContext.cookieMap()).thenReturn(cookieSet);
    accessLogEvent.setRoutingContext(mockContext);

    ELEMENT.appendServerFormattedItem(accessLogEvent, strBuilder);
    Assert.assertEquals("-", strBuilder.toString());
  }

  @Test
  public void clientFormattedElementOnCookieCountIsZero() {
    Map<String, Object> handlerMap = new HashMap<>();
    handlerMap.put(RestConst.INVOCATION_HANDLER_REQUESTCLIENT, restClientRequest);
    Map<String, String> cookieMap = new HashMap<>();

    when(finishEvent.getInvocation()).thenReturn(invocation);
    when(invocation.getHandlerContext()).thenReturn(handlerMap);
    when(restClientRequest.getCookieMap()).thenReturn(cookieMap);

    ELEMENT.appendClientFormattedItem(finishEvent, strBuilder);
    Assert.assertEquals("-", strBuilder.toString());
  }


  @Test
  public void serverFormattedElementOnCookieSetIsNull() {
    Mockito.when(mockContext.cookieCount()).thenReturn(1);
    Mockito.when(mockContext.cookieMap()).thenReturn(null);
    accessLogEvent.setRoutingContext(mockContext);

    ELEMENT.appendServerFormattedItem(accessLogEvent, strBuilder);
    Assert.assertEquals("-", strBuilder.toString());
  }

  @Test
  public void clientFormattedElementOnCookieSetIsNull() {
    Map<String, Object> handlerMap = new HashMap<>();
    handlerMap.put(RestConst.INVOCATION_HANDLER_REQUESTCLIENT, restClientRequest);

    when(finishEvent.getInvocation()).thenReturn(invocation);
    when(invocation.getHandlerContext()).thenReturn(handlerMap);
    when(restClientRequest.getCookieMap()).thenReturn(null);

    ELEMENT.appendClientFormattedItem(finishEvent, strBuilder);
    Assert.assertEquals("-", strBuilder.toString());
  }


  @Test
  public void serverFormattedElementOnNotFound() {
    HashMap<String, Cookie> cookieSet = new HashMap<>();
    CookieImpl cookie = new CookieImpl("anotherCookieName", COOKIE_VALUE);
    cookieSet.put(cookie.getName(), cookie);
    Mockito.when(mockContext.cookieCount()).thenReturn(1);
    Mockito.when(mockContext.cookieMap()).thenReturn(cookieSet);
    accessLogEvent.setRoutingContext(mockContext);

    ELEMENT.appendServerFormattedItem(accessLogEvent, strBuilder);
    Assert.assertEquals("-", strBuilder.toString());
  }

  @Test
  public void clientFormattedElementOnNotFound() {
    Map<String, Object> handlerMap = new HashMap<>();
    handlerMap.put(RestConst.INVOCATION_HANDLER_REQUESTCLIENT, restClientRequest);

    Map<String, String> cookieMap = new HashMap<>();
    cookieMap.put("anotherCookieValue", COOKIE_VALUE);
    when(finishEvent.getInvocation()).thenReturn(invocation);
    when(invocation.getHandlerContext()).thenReturn(handlerMap);
    when(restClientRequest.getCookieMap()).thenReturn(cookieMap);

    ELEMENT.appendClientFormattedItem(finishEvent, strBuilder);
    Assert.assertEquals("-", strBuilder.toString());
  }
}
