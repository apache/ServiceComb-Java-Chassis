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

import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultServerEndpointMetric;
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultTcpSocketMetric;

import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.TCPMetrics;

/**
 * important: not singleton, every NetServer instance relate to a TcpServerMetrics instance
 */
public class DefaultTcpServerMetrics implements TCPMetrics<DefaultTcpSocketMetric> {
  private final DefaultServerEndpointMetric endpointMetric;

  public DefaultTcpServerMetrics(DefaultServerEndpointMetric endpointMetric) {
    this.endpointMetric = endpointMetric;
  }

  public DefaultServerEndpointMetric getEndpointMetric() {
    return endpointMetric;
  }

  @Override
  public DefaultTcpSocketMetric connected(SocketAddress remoteAddress, String remoteName) {
    endpointMetric.onConnect();
    return new DefaultTcpSocketMetric(endpointMetric);
  }

  @Override
  public void disconnected(DefaultTcpSocketMetric socketMetric, SocketAddress remoteAddress) {
    socketMetric.onDisconnect();
  }

  @Override
  public void bytesRead(DefaultTcpSocketMetric socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    endpointMetric.addBytesRead(numberOfBytes);
  }

  @Override
  public void bytesWritten(DefaultTcpSocketMetric socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
    endpointMetric.addBytesWritten(numberOfBytes);
  }
}
