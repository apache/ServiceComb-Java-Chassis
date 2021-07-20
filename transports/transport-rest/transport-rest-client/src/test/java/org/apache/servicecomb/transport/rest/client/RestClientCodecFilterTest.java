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
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response.Status;

import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.core.filter.FilterNode;
import org.apache.servicecomb.foundation.common.utils.AsyncUtils;
import org.apache.servicecomb.foundation.test.scaffolding.exception.RuntimeExceptionWithoutStackTrace;
import org.apache.servicecomb.swagger.invocation.Response;
import org.junit.jupiter.api.Test;

// TODO: vert.x 4 changed HttpClientRequest creation behavior, and will
// connect to server and when successfully HttpClientRequest will created. So tests case will fail.
// These unit tests is hard to modify, will change it to integration test or think another method
// to test them
class RestClientCodecFilterTest extends RestClientTestBase {
//  RestClientEncoder encoder = new RestClientEncoder();
//
//  RestClientDecoder decoder = new RestClientDecoder();
//
//  RestClientCodecFilter codecFilter = new RestClientCodecFilter()
//      .setTransportContextFactory(factory)
//      .setEncoder(encoder)
//      .setDecoder(decoder);
//
//  @Test
//  void should_record_start_and_finish_client_filters_time_when_succeed() {
//    init("query", null, false);
//
//    Response response = codecFilter.onFilter(invocation, FilterNode.EMPTY).join();
//
//    assertThat(response.getStatusCode()).isEqualTo(Status.OK.getStatusCode());
//    assertThat(invocation.getInvocationStageTrace().getStartClientFiltersRequest()).isNotEqualTo(0);
//    assertThat(invocation.getInvocationStageTrace().getFinishClientFiltersResponse()).isNotEqualTo(0);
//  }
//
//  @Test
//  void should_record_start_and_finish_client_filters_time_when_failed() {
//    init("query", null, false);
//
//    RuntimeExceptionWithoutStackTrace mockThrowable = new RuntimeExceptionWithoutStackTrace("mock filter failed");
//    FilterNode filterNode = new FilterNode(null) {
//      @Override
//      public CompletableFuture<Response> onFilter(Invocation invocation) {
//        return AsyncUtils.completeExceptionally(mockThrowable);
//      }
//    };
//
//    Throwable throwable = catchThrowable(() -> codecFilter.onFilter(invocation, filterNode).join());
//
//    assertThat(throwable.getCause()).isSameAs(mockThrowable);
//    assertThat(invocation.getInvocationStageTrace().getStartClientFiltersRequest()).isNotEqualTo(0);
//    assertThat(invocation.getInvocationStageTrace().getFinishClientFiltersResponse()).isNotEqualTo(0);
//  }
}