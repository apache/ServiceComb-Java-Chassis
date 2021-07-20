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
import java.util.Enumeration;

import javax.ws.rs.core.HttpHeaders;

import org.apache.servicecomb.foundation.common.http.HttpUtils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;

public class VertxClientRequestToHttpServletRequest extends AbstractHttpServletRequest {
  private HttpClientRequest clientRequest;

  private String characterEncoding;

  public VertxClientRequestToHttpServletRequest(HttpClientRequest clientRequest, Buffer bodyBuffer) {
    this.clientRequest = clientRequest;
    setBodyBuffer(bodyBuffer);
  }

  @Override
  public String getRequestURI() {
    return clientRequest.path();
  }

  @Override
  public String getQueryString() {
    return clientRequest.query();
  }

  @Override
  public String getHeader(String name) {
    return clientRequest.headers().get(name);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    return Collections.enumeration(clientRequest.headers().getAll(name));
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return Collections.enumeration(clientRequest.headers().names());
  }

  @Override
  public void setHeader(String name, String value) {
    clientRequest.headers().set(name, value);
  }

  @Override
  public void addHeader(String name, String value) {
    clientRequest.headers().add(name, value);
  }

  @Override
  public String getContextPath() {
    return "";
  }

  @Override
  public String getMethod() {
    return clientRequest.getMethod().name();
  }

  @Override
  public String getContentType() {
    return clientRequest.headers().get(HttpHeaders.CONTENT_TYPE);
  }

  @Override
  public String getCharacterEncoding() {
    if (characterEncoding == null) {
      characterEncoding = HttpUtils.getCharsetFromContentType(getContentType());
    }

    return characterEncoding;
  }
}
