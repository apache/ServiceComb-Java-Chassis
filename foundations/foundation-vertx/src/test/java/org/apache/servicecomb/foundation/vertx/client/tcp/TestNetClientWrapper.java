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
package org.apache.servicecomb.foundation.vertx.client.tcp;


import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;

public class TestNetClientWrapper {
  @Mocked
  Vertx vertx;

  @Mocked
  TcpClientConfig normalClientConfig;

  @Mocked
  NetClient normalNetClient;

  @Mocked
  TcpClientConfig sslClientConfig;

  @Mocked
  NetClient sslNetClient;

  NetClientWrapper netClientWrapper;

  @Before
  public void setup() {
    new Expectations() {
      {
        vertx.createNetClient(normalClientConfig);
        result = normalNetClient;
        vertx.createNetClient(sslClientConfig);
        result = sslNetClient;
      }
    };
    netClientWrapper = new NetClientWrapper(vertx, normalClientConfig, sslClientConfig);
  }

  @Test
  public void getClientConfig() {
    Assert.assertSame(normalClientConfig, netClientWrapper.getClientConfig(false));
    Assert.assertSame(sslClientConfig, netClientWrapper.getClientConfig(true));
  }

  @Test
  public void connect(@Mocked NetSocket normalSocket, @Mocked NetSocket sslSocket) {
    int port = 8000;
    String host = "localhost";

    Promise<NetSocket> promiseConnect = Promise.promise();
    new MockUp<NetClient>(normalNetClient) {
      @Mock
      NetClient connect(int port, String host, Handler<AsyncResult<NetSocket>> connectHandler) {
        promiseConnect.complete(normalSocket);
        connectHandler.handle(promiseConnect.future());
        return null;
      }
    };

    Promise<NetSocket> sslPromiseConnect = Promise.promise();
    new MockUp<NetClient>(sslNetClient) {
      @Mock
      NetClient connect(int port, String host, Handler<AsyncResult<NetSocket>> connectHandler) {
        sslPromiseConnect.complete(sslSocket);
        connectHandler.handle(sslPromiseConnect.future());
        return null;
      }
    };

    List<NetSocket> socks = new ArrayList<>();
    netClientWrapper.connect(false, port, host, asyncSocket -> {
      socks.add(asyncSocket.result());
    });
    netClientWrapper.connect(true, port, host, asyncSocket -> {
      socks.add(asyncSocket.result());
    });

    Assert.assertThat(socks, Matchers.contains(normalSocket, sslSocket));
  }
}
