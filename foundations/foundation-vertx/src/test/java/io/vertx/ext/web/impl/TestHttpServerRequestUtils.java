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

package io.vertx.ext.web.impl;

import org.apache.servicecomb.foundation.vertx.http.VertxServerRequestToHttpServletRequest;
import org.junit.Assert;
import org.junit.Test;

import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.RoutingContext;
import mockit.Expectations;
import mockit.Mocked;

// HttpServerRequestWrapper is a package visible class, so put this test in package io.vertx.ext.web.impl
public class TestHttpServerRequestUtils {
  @Test
  public void testVertxServerRequestToHttpServletRequest(@Mocked RoutingContext context,
      @Mocked HttpServerRequestInternal request) {
    HttpServerRequestWrapper wrapper = new HttpServerRequestWrapper(request, AllowForwardHeaders.NONE);
    new Expectations() {
      {
        request.scheme();
        result = "http";
        context.request();
        result = wrapper;
      }
    };

    VertxServerRequestToHttpServletRequest reqEx = new VertxServerRequestToHttpServletRequest(context, "abc");
    Assert.assertEquals("abc", reqEx.getRequestURI());
  }
}
