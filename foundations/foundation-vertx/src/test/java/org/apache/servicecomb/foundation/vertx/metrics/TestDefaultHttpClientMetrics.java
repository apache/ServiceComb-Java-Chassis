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
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultEndpointMetric;
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultRequestMetric;
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultTcpSocketMetric;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.core.spi.observability.HttpRequest;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;

public class TestDefaultHttpClientMetrics {
  @Mocked
  Vertx vertx;

  VertxOptions vertxOptions = new VertxOptions();

  MetricsOptionsEx metricsOptionsEx = new MetricsOptionsEx();

  @Mocked
  HttpClient anyHttpClient;

  HttpClientOptions options = new HttpClientOptions();

  DefaultVertxMetrics defaultVertxMetrics;

  DefaultHttpClientMetrics clientMetrics_a;

  DefaultHttpClientMetrics clientMetrics_b;

  String host = "host";

  int port1 = 1;

  int port2 = 2;

  SocketAddress address1 = new SocketAddressImpl(port1, host);

  SocketAddress address2 = new SocketAddressImpl(port2, host);

  DefaultClientMetrics clientMetrics_a_1;

  DefaultEndpointMetric endpointMetric_a_1;

  DefaultTcpSocketMetric socketMetric_a_1;

  DefaultClientMetrics clientMetrics_a_2;

  DefaultEndpointMetric endpointMetric_a_2;

  DefaultTcpSocketMetric socketMetric_a_2;

  DefaultClientMetrics clientMetrics_b_1;

  DefaultEndpointMetric endpointMetric_b_1;

  DefaultTcpSocketMetric socketMetric_b_1;

  DefaultClientMetrics clientMetrics_b_2;

  DefaultEndpointMetric endpointMetric_b_2;

  DefaultTcpSocketMetric socketMetric_b_2;

  static long nanoTime;

  @BeforeClass
  public static void classSetup() {
    new MockUp<System>() {
      @Mock
      long nanoTime() {
        return nanoTime;
      }
    };
  }

  private static DefaultTcpSocketMetric initSocketMetric(DefaultClientMetrics clientMetrics,
      DefaultHttpClientMetrics metrics,
      SocketAddress address) {
    DefaultTcpSocketMetric socketMetric = metrics.connected(address, address.toString());
    metrics.endpointConnected(clientMetrics);
    return socketMetric;
  }

  @Before
  public void setup() {
    vertxOptions.setMetricsOptions(metricsOptionsEx);
    defaultVertxMetrics = new DefaultVertxMetrics(vertxOptions);
    defaultVertxMetrics.setVertx(vertx);
    clientMetrics_a = (DefaultHttpClientMetrics) defaultVertxMetrics.createHttpClientMetrics(options);
    clientMetrics_b = (DefaultHttpClientMetrics) defaultVertxMetrics.createHttpClientMetrics(options);

    nanoTime = 1;

    clientMetrics_a_1 = clientMetrics_a.createEndpointMetrics(address1, 0);
    socketMetric_a_1 = initSocketMetric(clientMetrics_a_1, clientMetrics_a, address1);
    endpointMetric_a_1 = socketMetric_a_1.getEndpointMetric();

    clientMetrics_a_2 = clientMetrics_a.createEndpointMetrics(address2, 0);
    socketMetric_a_2 = initSocketMetric(clientMetrics_a_2, clientMetrics_a, address2);
    endpointMetric_a_2 = socketMetric_a_2.getEndpointMetric();

    clientMetrics_b_1 = clientMetrics_b.createEndpointMetrics(address1, 0);
    socketMetric_b_1 = initSocketMetric(clientMetrics_b_1, clientMetrics_b, address1);
    endpointMetric_b_1 = socketMetric_b_1.getEndpointMetric();

    clientMetrics_b_2 = clientMetrics_b.createEndpointMetrics(address2, 0);
    socketMetric_b_2 = initSocketMetric(clientMetrics_b_2, clientMetrics_b, address2);
    endpointMetric_b_2 = socketMetric_b_2.getEndpointMetric();
  }

  @Test
  public void createMetrics() {
    Assert.assertNotSame(clientMetrics_a, clientMetrics_b);
  }

  @Test
  public void createEndpoint() {
    Assert.assertSame(endpointMetric_a_1, endpointMetric_b_1);
    Assert.assertNotSame(endpointMetric_a_1, endpointMetric_a_2);

    Assert.assertNotSame(endpointMetric_a_2, endpointMetric_b_1);
    Assert.assertSame(endpointMetric_a_2, endpointMetric_b_2);

    Assert.assertEquals(2, endpointMetric_a_1.getCurrentConnectionCount());
    Assert.assertEquals(2, endpointMetric_a_2.getCurrentConnectionCount());
  }

  @Test
  public void expire() {
    metricsOptionsEx.setCheckClientEndpointMetricExpiredInNano(10);

    nanoTime = 2;
    clientMetrics_a.disconnected(socketMetric_a_1, null);
    clientMetrics_a.disconnected(socketMetric_a_2, null);

    nanoTime = 13;
    defaultVertxMetrics.getClientEndpointMetricManager().onCheckClientEndpointMetricExpired(0);
    Assert.assertNotNull(
        defaultVertxMetrics.getClientEndpointMetricManager().getClientEndpointMetric(address1.toString()));
    Assert.assertNotNull(
        defaultVertxMetrics.getClientEndpointMetricManager().getClientEndpointMetric(address2.toString()));

    clientMetrics_b.disconnected(socketMetric_b_1, null);
    clientMetrics_b.disconnected(socketMetric_b_2, null);

    nanoTime = 23;
    defaultVertxMetrics.getClientEndpointMetricManager().onCheckClientEndpointMetricExpired(0);
    Assert.assertNotNull(
        defaultVertxMetrics.getClientEndpointMetricManager().getClientEndpointMetric(address1.toString()));
    Assert.assertNotNull(
        defaultVertxMetrics.getClientEndpointMetricManager().getClientEndpointMetric(address2.toString()));

    nanoTime = 24;
    defaultVertxMetrics.getClientEndpointMetricManager().onCheckClientEndpointMetricExpired(0);
    Assert
        .assertNull(defaultVertxMetrics.getClientEndpointMetricManager().getClientEndpointMetric(address1.toString()));
    Assert
        .assertNull(defaultVertxMetrics.getClientEndpointMetricManager().getClientEndpointMetric(address2.toString()));
  }

  @Test
  public void connect() {
    {
      Assert.assertSame(endpointMetric_a_1, socketMetric_a_1.getEndpointMetric());
      Assert.assertTrue(socketMetric_a_1.isConnected());
      Assert.assertEquals(1, socketMetric_a_1.getConnectedTime());
      Assert.assertEquals(2, socketMetric_a_1.getEndpointMetric().getConnectCount());
      Assert.assertEquals(0, socketMetric_a_1.getEndpointMetric().getDisconnectCount());
      Assert.assertEquals(2, socketMetric_a_1.getEndpointMetric().getCurrentConnectionCount());

      nanoTime = 2;
      clientMetrics_a.disconnected(socketMetric_a_1, null);

      Assert.assertEquals(2, ((DefaultClientEndpointMetric) endpointMetric_a_1).getLastNanoTime());
      Assert.assertFalse(socketMetric_a_1.isConnected());
      Assert.assertEquals(1, socketMetric_a_1.getConnectedTime());
      Assert.assertEquals(2, socketMetric_a_1.getEndpointMetric().getConnectCount());
      Assert.assertEquals(1, socketMetric_a_1.getEndpointMetric().getDisconnectCount());
      Assert.assertEquals(1, socketMetric_a_1.getEndpointMetric().getCurrentConnectionCount());
    }

    {
      Assert.assertSame(endpointMetric_a_2, socketMetric_a_2.getEndpointMetric());
      Assert.assertTrue(socketMetric_a_2.isConnected());
      Assert.assertEquals(1, socketMetric_a_2.getConnectedTime());
      Assert.assertEquals(2, socketMetric_a_2.getEndpointMetric().getConnectCount());
      Assert.assertEquals(0, socketMetric_a_2.getEndpointMetric().getDisconnectCount());
      Assert.assertEquals(2, socketMetric_a_2.getEndpointMetric().getCurrentConnectionCount());

      nanoTime = 4;
      clientMetrics_a.disconnected(socketMetric_a_2, null);

      Assert.assertEquals(4, ((DefaultClientEndpointMetric) endpointMetric_a_2).getLastNanoTime());
      Assert.assertFalse(socketMetric_a_2.isConnected());
      Assert.assertEquals(1, socketMetric_a_2.getConnectedTime());
      Assert.assertEquals(2, socketMetric_a_2.getEndpointMetric().getConnectCount());
      Assert.assertEquals(1, socketMetric_a_2.getEndpointMetric().getDisconnectCount());
      Assert.assertEquals(1, socketMetric_a_2.getEndpointMetric().getCurrentConnectionCount());
    }

    {
      Assert.assertSame(endpointMetric_b_1, socketMetric_b_1.getEndpointMetric());
      Assert.assertTrue(socketMetric_b_1.isConnected());
      Assert.assertEquals(1, socketMetric_b_1.getConnectedTime());
      Assert.assertEquals(2, socketMetric_b_1.getEndpointMetric().getConnectCount());
      Assert.assertEquals(1, socketMetric_b_1.getEndpointMetric().getDisconnectCount());
      Assert.assertEquals(1, socketMetric_b_1.getEndpointMetric().getCurrentConnectionCount());

      nanoTime = 6;
      clientMetrics_b.disconnected(socketMetric_b_1, null);

      Assert.assertEquals(6, ((DefaultClientEndpointMetric) endpointMetric_b_1).getLastNanoTime());
      Assert.assertFalse(socketMetric_b_1.isConnected());
      Assert.assertEquals(1, socketMetric_b_1.getConnectedTime());
      Assert.assertEquals(2, socketMetric_b_1.getEndpointMetric().getConnectCount());
      Assert.assertEquals(2, socketMetric_b_1.getEndpointMetric().getDisconnectCount());
      Assert.assertEquals(0, socketMetric_b_1.getEndpointMetric().getCurrentConnectionCount());
    }

    {
      Assert.assertSame(endpointMetric_b_2, socketMetric_b_2.getEndpointMetric());
      Assert.assertTrue(socketMetric_b_2.isConnected());
      Assert.assertEquals(1, socketMetric_b_2.getConnectedTime());
      Assert.assertEquals(2, socketMetric_b_2.getEndpointMetric().getConnectCount());
      Assert.assertEquals(1, socketMetric_b_2.getEndpointMetric().getDisconnectCount());
      Assert.assertEquals(1, socketMetric_b_2.getEndpointMetric().getCurrentConnectionCount());

      nanoTime = 7;
      clientMetrics_b.disconnected(socketMetric_b_2, null);

      Assert.assertEquals(7, ((DefaultClientEndpointMetric) endpointMetric_b_2).getLastNanoTime());
      Assert.assertFalse(socketMetric_b_2.isConnected());
      Assert.assertEquals(1, socketMetric_b_2.getConnectedTime());
      Assert.assertEquals(2, socketMetric_b_2.getEndpointMetric().getConnectCount());
      Assert.assertEquals(2, socketMetric_b_2.getEndpointMetric().getDisconnectCount());
      Assert.assertEquals(0, socketMetric_b_2.getEndpointMetric().getCurrentConnectionCount());
    }
  }

  @Test
  public void bytesReadAndWritten() {
    DefaultTcpSocketMetric socketMetric = clientMetrics_a.connected(address1, host);
    clientMetrics_a.endpointConnected(clientMetrics_a_1);
    clientMetrics_a.bytesRead(socketMetric, address1, 1);
    clientMetrics_a.bytesWritten(socketMetric, address1, 1);

    socketMetric = clientMetrics_a.connected(address2, host);
    clientMetrics_a.endpointConnected(clientMetrics_a_2);
    clientMetrics_a.bytesRead(socketMetric, address2, 1);
    clientMetrics_a.bytesWritten(socketMetric, address2, 1);

    socketMetric = clientMetrics_b.connected(address1, host);
    clientMetrics_b.endpointConnected(clientMetrics_b_1);
    clientMetrics_b.bytesRead(socketMetric, address1, 1);
    clientMetrics_b.bytesWritten(socketMetric, address1, 1);

    socketMetric = clientMetrics_b.connected(address2, host);
    clientMetrics_b.endpointConnected(clientMetrics_b_2);
    clientMetrics_b.bytesRead(socketMetric, address2, 1);
    clientMetrics_b.bytesWritten(socketMetric, address2, 1);

    Assert.assertEquals(2, endpointMetric_a_1.getBytesRead());
    Assert.assertEquals(2, endpointMetric_a_2.getBytesRead());
    Assert.assertEquals(2, endpointMetric_a_1.getBytesWritten());
    Assert.assertEquals(2, endpointMetric_a_2.getBytesWritten());
  }

  @Test
  public void requestBegin(@Mocked HttpRequest request) {
    DefaultTcpSocketMetric socketMetric = clientMetrics_a.connected(address1, host);

    nanoTime = 2;
    DefaultRequestMetric requestMetric = clientMetrics_a_1.requestBegin("/ui", request);
    nanoTime = 3;
    clientMetrics_a_1.requestEnd(requestMetric);

    Assert.assertEquals(2, requestMetric.getRequestBeginTime());
    Assert.assertEquals(3, requestMetric.getRequestEndTime());
  }
}
