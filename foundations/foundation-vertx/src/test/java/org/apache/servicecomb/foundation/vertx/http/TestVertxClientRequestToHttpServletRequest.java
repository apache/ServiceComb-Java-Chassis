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

package org.apache.servicecomb.foundation.vertx.http;

import java.util.Collections;

import javax.ws.rs.core.HttpHeaders;

import org.apache.servicecomb.foundation.common.http.HttpUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import mockit.Expectations;
import mockit.Mocked;

public class TestVertxClientRequestToHttpServletRequest {
  @Mocked
  HttpClientRequest clientRequest;

  Buffer bodyBuffer = Buffer.buffer();

  VertxClientRequestToHttpServletRequest request;

  @Before
  public void setup() {
    request = new VertxClientRequestToHttpServletRequest(clientRequest, bodyBuffer);
  }

  @Test
  public void testGetRequestURI() {
    new Expectations() {
      {
        clientRequest.path();
        result = "/path";
      }
    };

    Assert.assertEquals("/path", request.getRequestURI());
  }

  @Test
  public void testGetQueryString() {
    new Expectations() {
      {
        clientRequest.query();
        result = "a=1&b=2";
      }
    };

    Assert.assertEquals("a=1&b=2", request.getQueryString());
  }

  @Test
  public void testGetHeader() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("name", "value");
    new Expectations() {
      {
        clientRequest.headers();
        result = headers;
      }
    };

    Assert.assertEquals("value", request.getHeader("name"));
  }

  @Test
  public void testGetHeaders() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("name", "value");
    new Expectations() {
      {
        clientRequest.headers();
        result = headers;
      }
    };

    Assert.assertThat(Collections.list(request.getHeaders("name")), Matchers.contains("value"));
  }

  @Test
  public void testGetHeaderNames() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("name", "value");
    new Expectations() {
      {
        clientRequest.headers();
        result = headers;
      }
    };

    Assert.assertThat(Collections.list(request.getHeaderNames()), Matchers.contains("name"));
  }

  @Test
  public void testSetHeader() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    new Expectations() {
      {
        clientRequest.headers();
        result = headers;
      }
    };

    request.setHeader("name", "v1");
    request.setHeader("name", "v2");
    Assert.assertThat(headers.getAll("name"), Matchers.contains("v2"));
  }

  @Test
  public void testAddHeader() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    new Expectations() {
      {
        clientRequest.headers();
        result = headers;
      }
    };

    request.addHeader("name", "v1");
    request.addHeader("name", "v2");
    Assert.assertThat(headers.getAll("name"), Matchers.contains("v1", "v2"));
  }

  @Test
  public void testGetContextPath() {
    Assert.assertEquals("", request.getContextPath());
  }

  @Test
  public void getMethod() {
    new Expectations() {
      {
        clientRequest.getMethod();
        result = HttpMethod.GET;
      }
    };

    Assert.assertEquals("GET", request.getMethod());
  }

  @Test
  public void getContentType() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    new Expectations() {
      {
        clientRequest.headers();
        result = headers;
      }
    };

    request.addHeader(HttpHeaders.CONTENT_TYPE, "ct");

    Assert.assertEquals("ct", request.getContentType());
  }

  @Test
  public void getCharacterEncoding() {
    String contentType = "ct";
    String characterEncoding = "ce";

    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    new Expectations(HttpUtils.class) {
      {
        HttpUtils.getCharsetFromContentType(contentType);
        result = characterEncoding;
        clientRequest.headers();
        result = headers;
      }
    };

    request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);

    Assert.assertEquals("ce", request.getCharacterEncoding());
  }
}
