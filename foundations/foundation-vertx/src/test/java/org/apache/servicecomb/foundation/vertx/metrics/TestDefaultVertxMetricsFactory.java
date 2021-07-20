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

import org.junit.Assert;
import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.metrics.VertxMetrics;
import mockit.Mocked;

public class TestDefaultVertxMetricsFactory {
  @Mocked
  Vertx vertx;

  VertxOptions options = new VertxOptions();

  DefaultVertxMetricsFactory factory = new DefaultVertxMetricsFactory();

  @SuppressWarnings("deprecation")
  @Test
  public void metrics() {
    MetricsOptions metricsOptions = factory.newOptions();
    options.setMetricsOptions(metricsOptions);
    VertxMetrics vertxMetrics = factory.metrics(options);

    Assert.assertSame(factory, metricsOptions.getFactory());
    Assert.assertTrue(metricsOptions.isEnabled());

    Assert.assertSame(factory.getVertxMetrics(), vertxMetrics);
    Assert.assertTrue(vertxMetrics.isMetricsEnabled());
  }
}
