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

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.core.filter.ConsumerFilter;
import org.apache.servicecomb.core.filter.FilterNode;
import org.apache.servicecomb.swagger.invocation.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.http.HttpClientRequest;

@Component
public class RestClientCodecFilter implements ConsumerFilter {
  public static final String NAME = "rest-client-codec";

  protected RestClientTransportContextFactory transportContextFactory;

  protected RestClientEncoder encoder;

  protected RestClientDecoder decoder;

  @Nonnull
  @Override
  public String getName() {
    return NAME;
  }

  @Autowired
  public RestClientCodecFilter setTransportContextFactory(RestClientTransportContextFactory transportContextFactory) {
    this.transportContextFactory = transportContextFactory;
    return this;
  }

  @Autowired
  public RestClientCodecFilter setEncoder(RestClientEncoder encoder) {
    this.encoder = encoder;
    return this;
  }

  @Autowired
  public RestClientCodecFilter setDecoder(RestClientDecoder decoder) {
    this.decoder = decoder;
    return this;
  }

  @Override
  public CompletableFuture<Response> onFilter(Invocation invocation, FilterNode nextNode) {
    startClientFiltersRequest(invocation);
    // TODO: after upgrade vert.x , can use request metric to calculate request end time
    invocation.onStartSendRequest();
    //
    return CompletableFuture.completedFuture(null)
        .thenCompose(v -> transportContextFactory.createHttpClientRequest(invocation).toCompletionStage())
        .thenAccept(httpClientRequest -> prepareTransportContext(invocation, httpClientRequest))
        .thenAccept(v -> encoder.encode(invocation))
        .thenCompose(v -> nextNode.onFilter(invocation))
        .thenApply(response -> decoder.decode(invocation, response))
        .whenComplete((response, throwable) -> finishClientFiltersResponse(invocation));
  }

  protected void startClientFiltersRequest(Invocation invocation) {
    invocation.getInvocationStageTrace().startClientFiltersRequest();
  }

  protected void prepareTransportContext(Invocation invocation, HttpClientRequest httpClientRequest) {
    // TODO: after upgrade vert.x , can use request metric to calculate request end time
    invocation.getInvocationStageTrace().finishGetConnection(System.nanoTime());
    //

    RestClientTransportContext transportContext = transportContextFactory.create(invocation, httpClientRequest);
    invocation.setTransportContext(transportContext);
  }

  protected void finishClientFiltersResponse(Invocation invocation) {
    invocation.getInvocationStageTrace().finishClientFiltersResponse();
  }
}
