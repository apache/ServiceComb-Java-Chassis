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

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.VertxMetrics;

/**
 * <pre>
 * only for one vertx instance
 * DO NOT inject to vertx by SPI
 * typical usage:
 *     DefaultVertxMetricsFactory factory = new DefaultVertxMetricsFactory();
 *     MetricsOptionsEx metricsOptionsEx = (MetricsOptionsEx) factory.newOptions();
 *
 *     VertxOptions vertxOptions = new VertxOptions();
 *     vertxOptions.setMetricsOptions(metricsOptionsEx);
 *
 *     Vertx vertx = Vertx.vertx(vertxOptions);
 * </pre>
 */
public class DefaultVertxMetricsFactory implements VertxMetricsFactory {
  private DefaultVertxMetrics vertxMetrics;

  public DefaultVertxMetrics getVertxMetrics() {
    return vertxMetrics;
  }

  @Override
  public synchronized VertxMetrics metrics(VertxOptions options) {
    if (vertxMetrics == null) {
      vertxMetrics = new DefaultVertxMetrics(options);
    }
    return vertxMetrics;
  }

  @Override
  public MetricsOptions newOptions() {
    MetricsOptionsEx metricsOptions = new MetricsOptionsEx();
    metricsOptions.setFactory(this);
    metricsOptions.setEnabled(true);
    return metricsOptions;
  }

  @Override
  public MetricsOptions newOptions(JsonObject jsonObject) {
    return new MetricsOptionsEx(jsonObject);
  }

  @Override
  public MetricsOptions newOptions(MetricsOptions options) {
    return newOptions(options.toJson());
  }

  public void setVertx(Vertx vertx, VertxOptions options) {
    if (vertxMetrics == null) {
      vertxMetrics = new DefaultVertxMetrics(options);
    }
    vertxMetrics.setVertx(vertx);
  }
}
