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

package org.apache.servicecomb.common.rest.codec.param;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.servicecomb.common.rest.RestConst;
import org.apache.servicecomb.common.rest.codec.RestClientRequest;
import org.apache.servicecomb.common.rest.codec.param.BodyProcessorCreator.BodyProcessor;
import org.apache.servicecomb.common.rest.codec.param.BodyProcessorCreator.RawJsonBodyProcessor;
import org.apache.servicecomb.foundation.vertx.stream.BufferInputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.type.TypeFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;

public class TestBodyProcessor {
  @Mocked
  HttpServletRequest request;

  MultiMap headers;

  RestClientRequest clientRequest;

  ParamValueProcessor processor;

  ByteBuf inputBodyByteBuf = Unpooled.buffer();

  BufferInputStream inputStream = new BufferInputStream(inputBodyByteBuf);

  Buffer outputBodyBuffer;

  private void createProcessor(Class<?> type) {
    processor = new BodyProcessor(TypeFactory.defaultInstance().constructType(type), type.equals(String.class), true);
  }

  private void createRawJsonProcessor() {
    processor = new RawJsonBodyProcessor(TypeFactory.defaultInstance().constructType(String.class), true, true);
  }

  private void createClientRequest() {
    clientRequest = new MockUp<RestClientRequest>() {
      @Mock
      void putHeader(String name, String value) {
        headers.add(name, value);
      }

      @Mock
      void write(Buffer bodyBuffer) {
        outputBodyBuffer = bodyBuffer;
      }

      @Mock
      MultiMap getHeaders() {
        return headers;
      }
    }.getMockInstance();
  }

  private void initInputStream() throws IOException {
    new Expectations() {
      {
        request.getInputStream();
        result = inputStream;
      }
    };
  }

  private void setupGetValue(Class<?> type) throws IOException {
    createProcessor(type);
    initInputStream();
  }

  @Before
  public void before() {
    headers = new HeadersMultiMap();
  }

  @Test
  public void testGetValueHaveAttr() throws Exception {
    int body = 10;
    createProcessor(String.class);
    new Expectations() {
      {
        request.getAttribute(RestConst.BODY_PARAMETER);
        result = body;
      }
    };

    Object result = processor.getValue(request);
    Assert.assertEquals("10", result);
  }

  @Test
  public void testGetValueNoAttrNoStream() throws Exception {
    createProcessor(String.class);
    new Expectations() {
      {
        request.getInputStream();
        result = null;
      }
    };

    Object result = processor.getValue(request);
    Assert.assertNull(result);
  }

  @Test
  public void testGetValueTextPlain() throws Exception {
    setupGetValue(String.class);
    inputBodyByteBuf.writeCharSequence("abc", StandardCharsets.UTF_8);

    new Expectations() {
      {
        request.getContentType();
        result = MediaType.TEXT_PLAIN;
      }
    };

    Assert.assertEquals("abc", processor.getValue(request));
  }

  @Test
  public void testGetValueContextTypeJson() throws Exception {
    setupGetValue(Integer.class);
    inputBodyByteBuf.writeCharSequence("\"1\"", StandardCharsets.UTF_8);

    new Expectations() {
      {
        request.getContentType();
        result = MediaType.APPLICATION_JSON;
      }
    };

    Assert.assertEquals(1, processor.getValue(request));
  }

  @Test
  public void testGetValueDefaultJson() throws Exception {
    setupGetValue(Integer.class);
    inputBodyByteBuf.writeCharSequence("\"1\"", StandardCharsets.UTF_8);

    Assert.assertEquals(1, processor.getValue(request));
  }

  @Test
  public void testSetValue() throws Exception {
    createClientRequest();
    createProcessor(String.class);

    processor.setValue(clientRequest, "value");
    Assert.assertEquals(MediaType.APPLICATION_JSON, headers.get(HttpHeaders.CONTENT_TYPE));
    Assert.assertEquals("\"value\"", outputBodyBuffer.toString());
  }

  @Test
  public void testSetValueTextPlain() throws Exception {
    createClientRequest();
    createProcessor(String.class);
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);

    processor.setValue(clientRequest, "value");
    Assert.assertEquals(MediaType.TEXT_PLAIN, headers.get(HttpHeaders.CONTENT_TYPE));
    Assert.assertEquals("value", outputBodyBuffer.toString());
  }

  @Test
  public void testSetValueTextPlainTypeMismatch() {
    createClientRequest();
    createProcessor(String.class);
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);

    try {
      processor.setValue(clientRequest, new Date());
      fail("an exception is expected!");
    } catch (Exception e) {
      Assert.assertEquals(IllegalArgumentException.class, e.getClass());
      Assert.assertEquals("Content-Type is text/plain while arg type is not String", e.getMessage());
    }
  }

  @Test
  public void testGetParameterPath() {
    createProcessor(String.class);
    Assert.assertEquals("", processor.getParameterPath());
  }

  @Test
  public void testGetProcessorType() {
    createProcessor(String.class);
    Assert.assertEquals("body", processor.getProcessorType());
  }

  @Test
  public void testGetValueRawJson() throws Exception {
    createRawJsonProcessor();
    initInputStream();
    inputBodyByteBuf.writeCharSequence("\"1\"", StandardCharsets.UTF_8);

    Assert.assertEquals("\"1\"", processor.getValue(request));
  }

  @Test
  public void testGetValueRawJsonHaveAttr() throws Exception {
    int body = 10;
    createRawJsonProcessor();
    new Expectations() {
      {
        request.getAttribute(RestConst.BODY_PARAMETER);
        result = body;
      }
    };

    Object result = processor.getValue(request);
    Assert.assertEquals("10", result);
  }

  @Test
  public void testGetValueRawJsonNoAttrNoStream() throws Exception {
    createRawJsonProcessor();
    new Expectations() {
      {
        request.getInputStream();
        result = null;
      }
    };

    Object result = processor.getValue(request);
    Assert.assertNull(result);
  }

  @Test
  public void testSetValueRawJson() throws Exception {
    createClientRequest();
    createRawJsonProcessor();

    processor.setValue(clientRequest, "value");
    Assert.assertEquals(MediaType.APPLICATION_JSON, headers.get(HttpHeaders.CONTENT_TYPE));
    Assert.assertEquals("value", outputBodyBuffer.toString());
  }

  static class BodyModel {
    public String name;

    public int age;
  }

  @Test
  public void convertFromFormData() throws Exception {
    createProcessor(BodyModel.class);
    Map<String, String[]> parameterMap = new HashMap<>();
    parameterMap.put("name", new String[] {"n"});
    parameterMap.put("age", new String[] {"10"});
    new Expectations() {
      {
        request.getParameterMap();
        result = parameterMap;
        request.getContentType();
        result = MediaType.MULTIPART_FORM_DATA + ";utf-8";
      }
    };

    BodyModel bm = (BodyModel) processor.getValue(request);
    Assert.assertEquals("n", bm.name);
    Assert.assertEquals(10, bm.age);
  }

  @Test
  public void convertFromUrlencoded() throws Exception {
    createProcessor(BodyModel.class);
    Map<String, String[]> parameterMap = new HashMap<>();
    parameterMap.put("name", new String[] {"n"});
    parameterMap.put("age", new String[] {"10"});
    new Expectations() {
      {
        request.getParameterMap();
        result = parameterMap;
        request.getContentType();
        result = MediaType.APPLICATION_FORM_URLENCODED + ";utf-8";
      }
    };

    BodyModel bm = (BodyModel) processor.getValue(request);
    Assert.assertEquals("n", bm.name);
    Assert.assertEquals(10, bm.age);
  }
}
