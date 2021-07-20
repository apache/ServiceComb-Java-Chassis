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

package org.apache.servicecomb.foundation.vertx;

import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.impl.RoutingContextInternal;

public class TestSimpleBodyHandler {

  private SimpleBodyHandler instance;

  private RoutingContextInternal context;

  @Before
  public void setUp() throws Exception {
    context = Mockito.mock(RoutingContextInternal.class);
    HttpServerRequest request = Mockito.mock(HttpServerRequest.class);
    Mockito.when(context.request()).thenReturn(request);
    MultiMap multiMap = Mockito.mock(MultiMap.class);
    Mockito.when(request.headers()).thenReturn(multiMap);
    HttpServerResponse response = Mockito.mock(HttpServerResponse.class);
    Mockito.when(response.setStatusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode())).thenReturn(response);
    Mockito.when(context.response()).thenReturn(response);
  }

  @After
  public void tearDown() throws Exception {
    instance = null;
    context = null;
  }

  @Test
  public void testValidContentType() {
    instance = new SimpleBodyHandler() {
      @Override
      protected boolean contentTypeSupported(String contentType) {
        return true;
      }
    };
    instance.handle(context);
    Assert.assertTrue(instance.checkContentType(context));
  }

  @Test
  public void testInvalidContentType() {
    instance = new SimpleBodyHandler() {
      @Override
      protected boolean contentTypeSupported(String contentType) {
        return false;
      }
    };
    instance.handle(context);
    Assert.assertFalse(instance.checkContentType(context));
  }
}
