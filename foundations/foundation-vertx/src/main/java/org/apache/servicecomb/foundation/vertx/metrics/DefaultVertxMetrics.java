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

import java.util.Map;

import org.apache.servicecomb.foundation.common.concurrent.ConcurrentHashMapEx;
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultClientEndpointMetricManager;
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultServerEndpointMetric;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.metrics.impl.DummyVertxMetrics;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.metrics.TCPMetrics;

public class DefaultVertxMetrics extends DummyVertxMetrics {
  // to support listen multiple addresses, must use a map to manage the metric
  private final Map<String, DefaultServerEndpointMetric> serverEndpointMetricMap = new ConcurrentHashMapEx<>();

  private final DefaultClientEndpointMetricManager clientEndpointMetricManager;

  public DefaultVertxMetrics(VertxOptions vertxOptions) {
    this.clientEndpointMetricManager = new DefaultClientEndpointMetricManager(
        (MetricsOptionsEx) vertxOptions.getMetricsOptions());
  }

  public DefaultClientEndpointMetricManager getClientEndpointMetricManager() {
    return clientEndpointMetricManager;
  }

  public Map<String, DefaultServerEndpointMetric> getServerEndpointMetricMap() {
    return serverEndpointMetricMap;
  }

  @Override
  public HttpServerMetrics<?, ?, ?> createHttpServerMetrics(HttpServerOptions options, SocketAddress localAddress) {
    DefaultServerEndpointMetric endpointMetric = serverEndpointMetricMap
        .computeIfAbsent(localAddress.toString(), DefaultServerEndpointMetric::new);
    return new DefaultHttpServerMetrics(endpointMetric);
  }

  @Override
  public HttpClientMetrics<?, ?, ?, ?> createHttpClientMetrics(HttpClientOptions options) {
    return new DefaultHttpClientMetrics(clientEndpointMetricManager);
  }

  @Override
  public TCPMetrics<?> createNetServerMetrics(NetServerOptions options, SocketAddress localAddress) {
    DefaultServerEndpointMetric endpointMetric = serverEndpointMetricMap
        .computeIfAbsent(localAddress.toString(), DefaultServerEndpointMetric::new);
    return new DefaultTcpServerMetrics(endpointMetric);
  }

  @Override
  public TCPMetrics<?> createNetClientMetrics(NetClientOptions options) {
    return new DefaultTcpClientMetrics(clientEndpointMetricManager);
  }

  @Override
  public boolean isMetricsEnabled() {
    return true;
  }

  public void setVertx(Vertx vertx) {
    clientEndpointMetricManager.setVertx(vertx);
  }
}
