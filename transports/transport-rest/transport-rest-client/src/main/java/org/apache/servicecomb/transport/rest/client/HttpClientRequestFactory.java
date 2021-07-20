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

import org.apache.servicecomb.core.Invocation;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

/**
 * some service has special domain name rule, eg: k8s<br>
 * assume k8s domain name is https://k8s.com:1234, and clusterId is my-id<br>
 * then must send request to https://my-id.k8s.com:1234<br>
 * <br>
 * this interface allowed to modify host by invocation argument, eg:<br>
 * <pre>
 * {@code
 *  Future<HttpClientRequest> create(Invocation invocation, HttpClient httpClient, HttpMethod method, RequestOptions options) {
 *    if ("k8s".equals(invocation.getMicroserviceName())) {
 *      options.setHost(invocation.getSwaggerArgument("clusterId") + "." + options.getHost());
 *    }
 *
 *    return httpClient.request(options);
 *  }
 * }
 * </pre>
 */
public interface HttpClientRequestFactory {
  HttpClientRequestFactory DEFAULT = (invocation, httpClient, options) -> httpClient.request(options);

  Future<HttpClientRequest> create(Invocation invocation, HttpClient httpClient, RequestOptions options)
      throws Throwable;
}