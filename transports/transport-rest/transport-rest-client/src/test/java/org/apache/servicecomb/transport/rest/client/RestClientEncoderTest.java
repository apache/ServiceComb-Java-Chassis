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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Part;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.servicecomb.foundation.common.part.FilePart;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

// TODO: vert.x 4 changed HttpClientRequest creation behavior, and will
// connect to server and when successfully HttpClientRequest will created. So tests case will fail.
// These unit tests is hard to modify, will change it to integration test or think another method
// to test them
class RestClientEncoderTest extends RestClientTestBase {
//  RestClientEncoder encoder = new RestClientEncoder();
//
//  void init(String operationId, Map<String, Object> swaggerArgs) {
//    init(operationId, swaggerArgs, false);
//  }
//
//  @Test
//  void should_encode_header_parameter() {
//    init("header", ImmutableMap.of("header", "value"));
//
//    encoder.encode(invocation);
//
//    assertThat(httpClientRequest.headers().get("header"))
//        .isEqualTo("value");
//  }
//
//  @Test
//  void should_encode_servicecomb_headers() {
//    init("header", ImmutableMap.of("header", "value"));
//
//    encoder.encode(invocation);
//
//    assertThat(httpClientRequest.headers().toString())
//        .isEqualTo("header: value\n"
//            + "x-cse-target-microservice: defaultMicroservice\n"
//            + "x-cse-context: {\"x-cse-src-microservice\":\"defaultMicroservice\"}\n");
//  }
//
//  @Test
//  void should_not_encode_servicecomb_headers_when_invoke_3rd_service_and_filter_servicecomb_headers() {
//    init("header", ImmutableMap.of("header", "value"));
//    referenceConfig.setThirdPartyService(true);
//    operationMeta.getConfig().setClientRequestHeaderFilterEnabled(true);
//
//    encoder.encode(invocation);
//
//    assertThat(httpClientRequest.headers().toString())
//        .isEqualTo("header: value\n");
//  }
//
//  @Test
//  void should_encode_servicecomb_headers_when_invoke_3rd_service_and_not_filter_servicecomb_headers() {
//    init("header", ImmutableMap.of("header", "value"));
//    referenceConfig.setThirdPartyService(true);
//    operationMeta.getConfig().setClientRequestHeaderFilterEnabled(false);
//
//    encoder.encode(invocation);
//
//    assertThat(httpClientRequest.headers().toString())
//        .isEqualTo("header: value\n"
//            + "x-cse-target-microservice: defaultMicroservice\n"
//            + "x-cse-context: {\"x-cse-src-microservice\":\"defaultMicroservice\"}\n");
//  }
//
//  @Test
//  void should_encode_cookie_parameter() {
//    init("cookie", ImmutableMap.of("cookie1", "v1", "cookie2", "v2"));
//
//    encoder.encode(invocation);
//
//    assertThat(httpClientRequest.headers().get(HttpHeaders.COOKIE))
//        .isEqualTo("cookie1=v1; cookie2=v2");
//  }
//
//  @Test
//  void should_encode_body_parameter() {
//    init("body", ImmutableMap.of("body", "value"));
//
//    encoder.encode(invocation);
//
//    assertThat(transportContext.getRequestParameters().getBodyBuffer().toString())
//        .isEqualTo("\"value\"");
//  }
//
//  @Test
//  void should_encode_form_attribute_parameter() {
//    init("form", ImmutableMap.of("form1", "v1", "form2", "v2"));
//
//    encoder.encode(invocation);
//
//    assertThat(httpClientRequest.headers().get(HttpHeaders.CONTENT_TYPE))
//        .isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
//    assertThat(transportContext.getRequestParameters().getBodyBuffer().toString())
//        .isEqualTo("form1=v1&form2=v2");
//  }
//
//  @Test
//  void should_not_encode_null_form_attribute() {
//    Map<String, Object> swaggerArgs = new HashMap<>();
//    swaggerArgs.put("form1", "v1");
//    swaggerArgs.put("form2", null);
//    init("form", swaggerArgs);
//
//    encoder.encode(invocation);
//
//    assertThat(httpClientRequest.headers().get(HttpHeaders.CONTENT_TYPE))
//        .isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
//    assertThat(transportContext.getRequestParameters().getBodyBuffer().toString())
//        .isEqualTo("form1=v1");
//  }
//
//  @Test
//  void should_encode_form_with_upload_parameter() {
//    init("formWithUpload", ImmutableMap.of("form1", "v1", "form2", new File("form2")));
//
//    encoder.encode(invocation);
//
//    RestClientRequestParameters requestParameters = transportContext.getRequestParameters();
//    assertThat(requestParameters.getBodyBuffer().toString())
//        .isEqualTo("\r\n"
//            + "--my-boundary\r\n"
//            + "Content-Disposition: form-data; name=\"form1\"\r\n"
//            + "\r\n"
//            + "v1");
//    List<Part> parts = Lists.newArrayList(requestParameters.getUploads().get("form2"));
//    assertThat(parts).hasSize(1);
//    assertThat(((FilePart) parts.get(0))).isInstanceOf(FilePart.class);
//  }
//
//  @Test
//  void should_encode_form_with_upload_list() {
//    init("formWithUploadList", ImmutableMap.of("files", Arrays.asList(new File("f1"), new File("f2"))));
//
//    encoder.encode(invocation);
//
//    checkUploadList();
//  }
//
//  @Test
//  void should_encode_form_with_upload_array() {
//    init("formWithUploadList", ImmutableMap.of("files", new File[] {new File("f1"), new File("f2")}));
//
//    encoder.encode(invocation);
//
//    checkUploadList();
//  }
//
//  private void checkUploadList() {
//    RestClientRequestParameters requestParameters = transportContext.getRequestParameters();
//    assertThat(requestParameters.getBodyBuffer()).isNull();
//    List<Part> parts = Lists.newArrayList(requestParameters.getUploads().get("files"));
//    assertThat(parts).hasSize(2);
//    assertThat(((FilePart) parts.get(0)).getAbsolutePath()).endsWith("f1");
//    assertThat(((FilePart) parts.get(1)).getAbsolutePath()).endsWith("f2");
//  }
}