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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

// TODO: vert.x 4 changed HttpClientRequest creation behavior, and will
// connect to server and when successfully HttpClientRequest will created. So tests case will fail.
// These unit tests is hard to modify, will change it to integration test or think another method
// to test them
class RestClientTransportContextFactoryTest extends RestClientTestBase {
//  void init(Map<String, Object> swaggerArgs, boolean ssl) {
//    init("query", swaggerArgs, ssl);
//  }
//
//  @Test
//  void should_create_without_ssl() {
//    init(null, false);
//
//    assertThat(absoluteURI()).isEqualTo("http://localhost:1234/query");
//  }
//
//  @Test
//  void should_create_with_ssl() {
//    init(null, true);
//
//    assertThat(absoluteURI()).isEqualTo("https://localhost:1234/query");
//  }
//
//  @Test
//  void should_create_with_query() {
//    init(ImmutableMap.of("query", "value"), true);
//
//    assertThat(absoluteURI()).isEqualTo("https://localhost:1234/query?query=value");
//  }
//
//  @Test
//  void should_ignore_null_query_value() {
//    Map<String, Object> swaggerArgs = new HashMap<>();
//    swaggerArgs.put("query", null);
//    init(swaggerArgs, true);
//
//    assertThat(absoluteURI()).isEqualTo("https://localhost:1234/query");
//  }
//
//  @Test
//  void should_create_with_query_list() {
//    init(ImmutableMap.of("query", Arrays.asList("v1", "v2")), true);
//
//    assertThat(absoluteURI()).isEqualTo("https://localhost:1234/query?query=v1&query=v2");
//  }
//
//  @Test
//  void should_ignore_null_in_query_list() {
//    init(ImmutableMap.of("query", Arrays.asList("v1", null)), true);
//
//    assertThat(absoluteURI()).isEqualTo("https://localhost:1234/query?query=v1");
//  }
//
//  @Test
//  void should_create_with_query_array() {
//    init(ImmutableMap.of("query", new String[] {"v1", "v2"}), true);
//
//    assertThat(absoluteURI()).isEqualTo("https://localhost:1234/query?query=v1&query=v2");
//  }
//
//  @Test
//  void should_ignore_null_in_query_array() {
//    init(ImmutableMap.of("query", new String[] {"v1", null}), true);
//
//    assertThat(absoluteURI()).isEqualTo("https://localhost:1234/query?query=v1");
//  }
//
//  @Test
//  void should_get_local_address_as_not_connected_before_connect() {
//    init(null, true);
//
//    assertThat(transportContext.getLocalAddress()).isEqualTo("not connected");
//  }
//
//  @Test
//  void should_allowed_modify_host() {
//    factory.setHttpClientRequestFactory((invocation, httpClient, options) -> {
//      options.setHost(invocation.getSwaggerArgument("clusterId") + "." + options.getHost());
//      return httpClient.request(options).result();
//    });
//
//    init(ImmutableMap.of("clusterId", "my-id"), true);
//
//    assertThat(absoluteURI()).isEqualTo("https://my-id.localhost:1234/query");
//
//    factory.setHttpClientRequestFactory(HttpClientRequestFactory.DEFAULT);
//  }
}