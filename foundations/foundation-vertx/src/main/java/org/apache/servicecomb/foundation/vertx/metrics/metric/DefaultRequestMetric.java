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

package org.apache.servicecomb.foundation.vertx.metrics.metric;

public class DefaultRequestMetric {
  private long requestBeginTime;

  private long requestEndTime;

  private long responseBeginTime;

  private long responseEndTime;

  private final DefaultEndpointMetric endpointMetric;

  public DefaultRequestMetric(DefaultEndpointMetric endpointMetric) {
    this.endpointMetric = endpointMetric;
  }

  public long getRequestBeginTime() {
    return requestBeginTime != 0 ? requestBeginTime : System.nanoTime();
  }

  public long getRequestEndTime() {
    return requestEndTime != 0 ? requestEndTime : System.nanoTime();
  }

  public long getResponseBeginTime() {
    return responseBeginTime != 0 ? responseBeginTime : System.nanoTime();
  }

  public long getResponseEndTime() {
    return responseEndTime != 0 ? responseEndTime : System.nanoTime();
  }

  public void requestBegin() {
    this.requestBeginTime = System.nanoTime();
  }

  public void requestEnd() {
    this.requestEndTime = System.nanoTime();
  }

  public void responseBegin() {
    this.responseBeginTime = System.nanoTime();
  }

  public void responseEnd() {
    this.responseEndTime = System.nanoTime();
    this.endpointMetric.incrementRequests();
    this.endpointMetric.addLatency(System.nanoTime() - this.requestBeginTime);
  }
}
