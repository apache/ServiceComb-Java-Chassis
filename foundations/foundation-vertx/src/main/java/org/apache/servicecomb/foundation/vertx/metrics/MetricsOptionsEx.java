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

import java.util.concurrent.TimeUnit;

import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;

public class MetricsOptionsEx extends MetricsOptions {
  private long checkClientEndpointMetricIntervalInMilliseconds = TimeUnit.MINUTES.toMillis(1);

  private long checkClientEndpointMetricExpiredInNano = TimeUnit.MINUTES.toNanos(15);

  public MetricsOptionsEx() {
    super();
  }

  public MetricsOptionsEx(JsonObject json) {
    super(json);
  }

  public long getCheckClientEndpointMetricIntervalInMilliseconds() {
    return checkClientEndpointMetricIntervalInMilliseconds;
  }

  public void setCheckClientEndpointMetricIntervalInMinute(long minute) {
    this.checkClientEndpointMetricIntervalInMilliseconds = TimeUnit.MINUTES.toMillis(minute);
  }

  public long getCheckClientEndpointMetricExpiredInNano() {
    return checkClientEndpointMetricExpiredInNano;
  }

  public void setCheckClientEndpointMetricExpiredInNano(long nanoTime) {
    this.checkClientEndpointMetricExpiredInNano = nanoTime;
  }

  public void setCheckClientEndpointMetricExpiredInMinute(long minute) {
    this.checkClientEndpointMetricExpiredInNano = TimeUnit.MINUTES.toNanos(minute);
  }
}
