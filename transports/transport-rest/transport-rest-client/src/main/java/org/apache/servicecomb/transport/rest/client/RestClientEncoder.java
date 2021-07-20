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
package org.apache.servicecomb.transport.rest.client;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.apache.servicecomb.transport.rest.client.RestClientExceptionCodes.FAILED_TO_ENCODE_REST_CLIENT_REQUEST;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.Part;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.servicecomb.common.rest.codec.RestCodec;
import org.apache.servicecomb.common.rest.codec.RestObjectMapperFactory;
import org.apache.servicecomb.common.rest.codec.query.QueryCodec;
import org.apache.servicecomb.core.Const;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.core.definition.OperationConfig;
import org.apache.servicecomb.foundation.common.utils.StringBuilderUtils;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;

/**
 * encode all send data except upload
 */
@Component
public class RestClientEncoder {
  private static final Logger LOGGER = LoggerFactory.getLogger(RestClientEncoder.class);

  public static final int FORM_BUFFER_SIZE = 1024;

  public void encode(Invocation invocation) {
    try {
      EncoderSession encoderSession = new EncoderSession(invocation);
      encoderSession.doEncode();
    } catch (Exception e) {
      throw new InvocationException(BAD_REQUEST, FAILED_TO_ENCODE_REST_CLIENT_REQUEST, e.getMessage(), e);
    }
  }

  public static class EncoderSession {
    protected final Invocation invocation;

    protected final RestClientTransportContext transportContext;

    protected final RestClientRequestParameters requestParameters;

    protected final HttpClientRequest httpClientRequest;

    public EncoderSession(Invocation invocation) {
      this.invocation = invocation;
      this.transportContext = invocation.getTransportContext();
      this.requestParameters = this.transportContext.getRequestParameters();
      this.httpClientRequest = this.transportContext.getHttpClientRequest();
    }

    protected void doEncode() throws Exception {
      RestClientEncoder.LOGGER.debug("encode rest client request, operation={}, method={}, endpoint={}, uri={}.",
          invocation.getMicroserviceQualifiedName(),
          httpClientRequest.getMethod(),
          invocation.getEndpoint().getEndpoint(),
          httpClientRequest.getURI());

      swaggerArgumentsToRequest();

      writeCookies(requestParameters.getCookieMap());
      writeScbHeaders();
      writeForm(requestParameters.getFormMap());
    }

    protected void swaggerArgumentsToRequest() throws Exception {
      RestCodec
          .argsToRest(invocation.getSwaggerArguments(), transportContext.getRestOperationMeta(), requestParameters);
    }

    protected void writeCookies(@Nullable Map<String, String> cookieMap) {
      if (CollectionUtils.isEmpty(cookieMap)) {
        return;
      }

      StringBuilder builder = new StringBuilder();
      for (Entry<String, String> entry : cookieMap.entrySet()) {
        builder.append(entry.getKey())
            .append('=')
            .append(entry.getValue())
            .append("; ");
      }
      StringBuilderUtils.deleteLast(builder, 2);
      httpClientRequest.putHeader(HttpHeaders.COOKIE, builder.toString());
    }

    protected void writeScbHeaders() throws JsonProcessingException {
      OperationConfig operationConfig = invocation.getOperationMeta().getConfig();
      if (invocation.isThirdPartyInvocation() && operationConfig.isClientRequestHeaderFilterEnabled()) {
        return;
      }

      httpClientRequest.putHeader(Const.TARGET_MICROSERVICE, invocation.getMicroserviceName());
      httpClientRequest.putHeader(Const.CSE_CONTEXT,
          RestObjectMapperFactory.getRestObjectMapper().writeValueAsString(invocation.getContext()));
    }

    protected void writeForm(@Nullable Map<String, Object> formMap) throws Exception {
      if (requestParameters.getUploads() == null) {
        writeUrlEncodedForm(formMap);
        return;
      }

      writeChunkedForm(formMap);
    }

    protected void writeUrlEncodedForm(@Nullable Map<String, Object> formMap) throws Exception {
      if (formMap == null) {
        return;
      }

      httpClientRequest.putHeader(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

      Buffer bodyBuffer = genUrlEncodedFormBuffer(formMap);
      requestParameters.setBodyBuffer(bodyBuffer);
    }

    protected Buffer genUrlEncodedFormBuffer(@Nonnull Map<String, Object> formMap) throws Exception {
      // 2x faster than UriComponentsBuilder
      ByteBuf byteBuf = Unpooled.buffer(RestClientEncoder.FORM_BUFFER_SIZE);
      for (Entry<String, Object> entry : formMap.entrySet()) {
        writeCharSequence(byteBuf, entry.getKey());
        byteBuf.writeByte('=');

        String value = QueryCodec.convertToString(entry.getValue());
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        writeCharSequence(byteBuf, encodedValue);

        byteBuf.markWriterIndex();
        byteBuf.writeByte('&');
      }

      byteBuf.resetWriterIndex();
      return Buffer.buffer(byteBuf);
    }

    protected void writeChunkedForm(@Nullable Map<String, Object> formMap) throws Exception {
      String boundary = transportContext.getOrCreateBoundary();

      httpClientRequest.setChunked(true);
      httpClientRequest.putHeader(CONTENT_TYPE, MULTIPART_FORM_DATA + "; charset=UTF-8; boundary=" + boundary);

      if (formMap == null) {
        return;
      }

      Buffer bodyBuffer = genChunkedFormBuffer(formMap, boundary);
      requestParameters.setBodyBuffer(bodyBuffer);
    }

    protected Buffer genChunkedFormBuffer(@Nonnull Map<String, Object> formMap, String boundary) throws Exception {
      ByteBuf byteBuf = Unpooled.buffer(RestClientEncoder.FORM_BUFFER_SIZE);
      for (Entry<String, Object> entry : formMap.entrySet()) {
        writeCharSequence(byteBuf, "\r\n--");
        writeCharSequence(byteBuf, boundary);
        writeCharSequence(byteBuf, "\r\nContent-Disposition: form-data; name=\"");
        writeCharSequence(byteBuf, entry.getKey());
        writeCharSequence(byteBuf, "\"\r\n\r\n");

        String value = QueryCodec.convertToString(entry.getValue());
        writeCharSequence(byteBuf, value);
      }
      return Buffer.buffer(byteBuf);
    }
  }

  protected static void writeCharSequence(ByteBuf byteBuf, String value) {
    byteBuf.writeCharSequence(value, StandardCharsets.UTF_8);
  }

  public static Buffer genFileBoundaryBuffer(Part part, String name, String boundary) {
    ByteBuf byteBuf = Unpooled.buffer();

    writeCharSequence(byteBuf, "\r\n--");
    writeCharSequence(byteBuf, boundary);
    writeCharSequence(byteBuf, "\r\nContent-Disposition: form-data; name=\"");
    writeCharSequence(byteBuf, name);
    writeCharSequence(byteBuf, "\"; filename=\"");
    writeCharSequence(byteBuf, String.valueOf(part.getSubmittedFileName()));
    writeCharSequence(byteBuf, "\"\r\n");

    writeCharSequence(byteBuf, "Content-Type: ");
    writeCharSequence(byteBuf, part.getContentType());
    writeCharSequence(byteBuf, "\r\n");

    writeCharSequence(byteBuf, "Content-Transfer-Encoding: binary\r\n");

    writeCharSequence(byteBuf, "\r\n");

    return Buffer.buffer(byteBuf);
  }

  public static Buffer genBoundaryEndBuffer(String boundary) {
    ByteBuf byteBuf = Unpooled.buffer();

    writeCharSequence(byteBuf, "\r\n--");
    writeCharSequence(byteBuf, boundary);
    writeCharSequence(byteBuf, "--\r\n");

    return Buffer.buffer(byteBuf);
  }
}
