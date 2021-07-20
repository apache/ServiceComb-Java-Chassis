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

package org.apache.servicecomb.foundation.vertx.metrics;

import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultClientEndpointMetric;
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultClientTaskMetric;
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultRequestMetric;

import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;

public class DefaultClientMetrics implements
    ClientMetrics<DefaultRequestMetric, DefaultClientTaskMetric, HttpRequest, HttpResponse> {
  private final DefaultClientEndpointMetric clientEndpointMetric;

  public DefaultClientMetrics(DefaultClientEndpointMetric clientEndpointMetric) {
    this.clientEndpointMetric = clientEndpointMetric;
  }

  public DefaultClientEndpointMetric getClientEndpointMetric() {
    return this.clientEndpointMetric;
  }

  @Override
  public DefaultClientTaskMetric enqueueRequest() {
    DefaultClientTaskMetric taskMetric = new DefaultClientTaskMetric();
    taskMetric.enqueueRequest();
    return taskMetric;
  }

  @Override
  public void dequeueRequest(DefaultClientTaskMetric endpointMetric) {
    endpointMetric.dequeueRequest();
  }

  @Override
  public DefaultRequestMetric requestBegin(String uri, HttpRequest request) {
    DefaultRequestMetric requestMetric = new DefaultRequestMetric(this.clientEndpointMetric);
    requestMetric.requestBegin();
    return requestMetric;
  }

  @Override
  public void requestEnd(DefaultRequestMetric requestMetric, long bytesWritten) {
    requestMetric.requestEnd();
  }

  @Override
  public void responseBegin(DefaultRequestMetric requestMetric, HttpResponse response) {
    requestMetric.responseBegin();
  }

  @Override
  public void responseEnd(DefaultRequestMetric requestMetric, long bytesRead) {
    requestMetric.responseEnd();
  }
}
