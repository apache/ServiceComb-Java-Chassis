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

import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultEndpointMetric;
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultTcpSocketMetric;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SocketAddress;
import mockit.Mocked;

public class TestDefaultHttpServerMetrics {
  @Mocked
  Vertx vertx;

  VertxOptions vertxOptions = new VertxOptions();

  MetricsOptionsEx metricsOptionsEx = new MetricsOptionsEx();

  DefaultVertxMetrics defaultVertxMetrics;

  @Mocked
  HttpServer listen1_server1;

  @Mocked
  HttpServer listen1_server2;

  @Mocked
  SocketAddress listen1_addr;

  @Mocked
  HttpServer listen2_server1;

  @Mocked
  HttpServer listen2_server2;

  @Mocked
  SocketAddress listen2_addr;

  @Mocked
  HttpServerOptions options;

  @Mocked
  SocketAddress anyRemoteAddr;

  DefaultHttpServerMetrics metrics_listen1_server1;

  DefaultHttpServerMetrics metrics_listen1_server2;

  DefaultEndpointMetric endpointMetric1;

  DefaultHttpServerMetrics metrics_listen2_server1;

  DefaultHttpServerMetrics metrics_listen2_server2;

  DefaultEndpointMetric endpointMetric2;

  String remoteName = "remote";

  DefaultTcpSocketMetric socketMetric_listen1_1;

  DefaultTcpSocketMetric socketMetric_listen1_2;

  DefaultTcpSocketMetric socketMetric_listen2_1;

  DefaultTcpSocketMetric socketMetric_listen2_2;

  DefaultTcpSocketMetric socketMetric_listen2_3;

  @Before
  public void setup() {
    vertxOptions.setMetricsOptions(metricsOptionsEx);
    defaultVertxMetrics = new DefaultVertxMetrics(vertxOptions);

    metrics_listen1_server1 = (DefaultHttpServerMetrics) defaultVertxMetrics
        .createHttpServerMetrics(options, listen1_addr);
    metrics_listen1_server2 = (DefaultHttpServerMetrics) defaultVertxMetrics
        .createHttpServerMetrics(options, listen1_addr);
    endpointMetric1 = metrics_listen1_server1.getEndpointMetric();

    metrics_listen2_server1 = (DefaultHttpServerMetrics) defaultVertxMetrics
        .createHttpServerMetrics(options, listen2_addr);
    metrics_listen2_server2 = (DefaultHttpServerMetrics) defaultVertxMetrics
        .createHttpServerMetrics(options, listen2_addr);
    endpointMetric2 = metrics_listen2_server1.getEndpointMetric();

    socketMetric_listen1_1 = metrics_listen1_server1.connected(anyRemoteAddr, remoteName);
    socketMetric_listen1_2 = metrics_listen1_server2.connected(anyRemoteAddr, remoteName);
    socketMetric_listen2_1 = metrics_listen2_server1.connected(anyRemoteAddr, remoteName);
    socketMetric_listen2_2 = metrics_listen2_server2.connected(anyRemoteAddr, remoteName);
    socketMetric_listen2_3 = metrics_listen2_server2.connected(anyRemoteAddr, remoteName);
  }

  @Test
  public void createMetrics() {
    Map<Object, Object> instances = new IdentityHashMap<>();
    instances.put(metrics_listen1_server1, null);
    instances.put(metrics_listen1_server2, null);
    instances.put(metrics_listen2_server1, null);
    instances.put(metrics_listen2_server2, null);
    Assert.assertEquals(4, instances.size());

    Assert.assertSame(metrics_listen1_server1.getEndpointMetric(), metrics_listen1_server2.getEndpointMetric());
    Assert.assertNotSame(metrics_listen1_server1.getEndpointMetric(), metrics_listen2_server1.getEndpointMetric());
    Assert.assertSame(metrics_listen2_server1.getEndpointMetric(), metrics_listen2_server2.getEndpointMetric());
  }

  @Test
  public void connectionCount() {
    Map<Object, Object> instances = new IdentityHashMap<>();
    instances.put(socketMetric_listen1_1, null);
    instances.put(socketMetric_listen1_2, null);
    instances.put(socketMetric_listen2_1, null);
    instances.put(socketMetric_listen2_2, null);
    instances.put(socketMetric_listen2_3, null);
    Assert.assertEquals(5, instances.size());

    Assert.assertTrue(socketMetric_listen1_1.isConnected());
    Assert.assertTrue(socketMetric_listen1_2.isConnected());
    Assert.assertTrue(socketMetric_listen2_1.isConnected());
    Assert.assertTrue(socketMetric_listen2_2.isConnected());
    Assert.assertTrue(socketMetric_listen2_3.isConnected());

    Assert.assertEquals(2, endpointMetric1.getCurrentConnectionCount());
    Assert.assertEquals(3, endpointMetric2.getCurrentConnectionCount());

    // disconnect
    metrics_listen1_server1.disconnected(socketMetric_listen1_1, anyRemoteAddr);
    metrics_listen1_server2.disconnected(socketMetric_listen1_2, anyRemoteAddr);
    metrics_listen2_server1.disconnected(socketMetric_listen2_1, anyRemoteAddr);
    metrics_listen2_server2.disconnected(socketMetric_listen2_2, anyRemoteAddr);
    metrics_listen2_server2.disconnected(socketMetric_listen2_3, anyRemoteAddr);

    Assert.assertFalse(socketMetric_listen1_1.isConnected());
    Assert.assertFalse(socketMetric_listen1_2.isConnected());
    Assert.assertFalse(socketMetric_listen2_1.isConnected());
    Assert.assertFalse(socketMetric_listen2_2.isConnected());
    Assert.assertFalse(socketMetric_listen2_3.isConnected());

    Assert.assertEquals(0, endpointMetric1.getCurrentConnectionCount());
    Assert.assertEquals(0, endpointMetric2.getCurrentConnectionCount());
  }

  @Test
  public void bytesRead() {
    metrics_listen1_server1.bytesRead(socketMetric_listen1_1, anyRemoteAddr, 1);
    metrics_listen1_server2.bytesRead(socketMetric_listen1_2, anyRemoteAddr, 2);
    metrics_listen2_server1.bytesRead(socketMetric_listen2_1, anyRemoteAddr, 3);
    metrics_listen2_server2.bytesRead(socketMetric_listen2_2, anyRemoteAddr, 4);
    metrics_listen2_server2.bytesRead(socketMetric_listen2_3, anyRemoteAddr, 5);

    Assert.assertEquals(3, endpointMetric1.getBytesRead());
    Assert.assertEquals(12, endpointMetric2.getBytesRead());
  }

  @Test
  public void bytesWritten() {
    metrics_listen1_server1.bytesWritten(socketMetric_listen1_1, anyRemoteAddr, 1);
    metrics_listen1_server2.bytesWritten(socketMetric_listen1_2, anyRemoteAddr, 2);
    metrics_listen2_server1.bytesWritten(socketMetric_listen2_1, anyRemoteAddr, 3);
    metrics_listen2_server2.bytesWritten(socketMetric_listen2_2, anyRemoteAddr, 4);
    metrics_listen2_server2.bytesWritten(socketMetric_listen2_3, anyRemoteAddr, 5);

    Assert.assertEquals(3, endpointMetric1.getBytesWritten());
    Assert.assertEquals(12, endpointMetric2.getBytesWritten());
  }
}
