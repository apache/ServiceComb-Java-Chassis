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

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.servicecomb.foundation.test.scaffolding.exception.RuntimeExceptionWithoutStackTrace;
import org.apache.servicecomb.foundation.vertx.client.tcp.TcpClientConnection.Status;
import org.apache.servicecomb.foundation.vertx.tcp.TcpOutputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.impl.NetSocketImpl;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;

public class TestTcpClientConnection {
  @Mocked
  Context context;

  @Mocked
  NetClientWrapper netClientWrapper;

  String strEndpoint = "rest://localhost:8080";

  TcpClientConnection tcpClientConnection;

  Map<Long, TcpRequest> requestMap;

  Queue<ByteBuf> writeQueue;

  Queue<AbstractTcpClientPackage> packageQueue;

  @Before
  public void setup() {
    tcpClientConnection = new TcpClientConnection(context, netClientWrapper, strEndpoint);
    requestMap = Deencapsulation.getField(tcpClientConnection, "requestMap");
    packageQueue = Deencapsulation.getField(tcpClientConnection, "packageQueue");
    writeQueue = Deencapsulation.getField(tcpClientConnection, "writeQueue");
  }

  @Test
  public void localSupportLogin() {
    Assert.assertFalse(tcpClientConnection.isLocalSupportLogin());

    tcpClientConnection.setLocalSupportLogin(true);
    Assert.assertTrue(tcpClientConnection.isLocalSupportLogin());
  }

  @Test
  public void createLogin() {
    Assert.assertNull(tcpClientConnection.createLogin());
  }

  @Test
  public void onLoginResponse_buffer() {
    Assert.assertTrue(tcpClientConnection.onLoginResponse(null));
  }

  @Test
  public void send_inWorkingStatus(@Mocked AbstractTcpClientPackage tcpClientPackage,
      @Mocked TcpOutputStream tcpOutputStream) {
    Deencapsulation.setField(tcpClientConnection, "status", Status.WORKING);

    long msgId = 1;
    ByteBuf byteBuf = Unpooled.buffer();
    new Expectations(tcpClientConnection) {
      {
        tcpClientPackage.getMsgId();
        result = msgId;
        tcpClientPackage.createStream();
        result = tcpOutputStream;
        tcpOutputStream.getByteBuf();
        result = byteBuf;
      }
    };

    new MockUp<Context>(context) {
      @Mock
      void runOnContext(Handler<Void> action) {
      }
    };
    tcpClientConnection.send(tcpClientPackage, ar -> {
    });

    Assert.assertSame(byteBuf, writeQueue.poll());
    Assert.assertNull(writeQueue.poll());
    Assert.assertEquals(Status.WORKING, Deencapsulation.getField(tcpClientConnection, "status"));
  }

  @Test
  public void send_inDisconnectedStatus(@Mocked AbstractTcpClientPackage tcpClientPackage,
      @Mocked TcpOutputStream tcpOutputStream) {
    long msgId = 1;
    new Expectations(tcpClientConnection) {
      {
        tcpClientPackage.getMsgId();
        result = msgId;
      }
    };
    new MockUp<Context>(context) {
      @Mock
      void runOnContext(Handler<Void> action) {
        action.handle(null);
      }
    };
    tcpClientConnection.send(tcpClientPackage, ar -> {
    });

    Assert.assertSame(tcpClientPackage, packageQueue.poll());
    Assert.assertNull(packageQueue.poll());
    Assert.assertEquals(Status.CONNECTING, Deencapsulation.getField(tcpClientConnection, "status"));
  }

  @Test
  public void send_disconnectedToTryLogin(@Mocked AbstractTcpClientPackage tcpClientPackage,
      @Mocked TcpOutputStream tcpOutputStream) {
    long msgId = 1;
    new Expectations(tcpClientConnection) {
      {
        tcpClientPackage.getMsgId();
        result = msgId;
      }
    };
    new MockUp<Context>(context) {
      @Mock
      void runOnContext(Handler<Void> action) {
        Deencapsulation.setField(tcpClientConnection, "status", Status.TRY_LOGIN);
        action.handle(null);
      }
    };
    tcpClientConnection.send(tcpClientPackage, ar -> {
    });

    Assert.assertSame(tcpClientPackage, packageQueue.poll());
    Assert.assertNull(packageQueue.poll());
    Assert.assertEquals(Status.TRY_LOGIN, Deencapsulation.getField(tcpClientConnection, "status"));
  }

  @Test
  public void send_disconnectedToWorking(@Mocked AbstractTcpClientPackage tcpClientPackage,
      @Mocked TcpOutputStream tcpOutputStream) {
    long msgId = 1;
    new Expectations(tcpClientConnection) {
      {
        tcpClientPackage.getMsgId();
        result = msgId;
        tcpClientConnection.write((ByteBuf) any);
      }
    };
    new MockUp<Context>(context) {
      @Mock
      void runOnContext(Handler<Void> action) {
        Deencapsulation.setField(tcpClientConnection, "status", Status.WORKING);
        action.handle(null);
      }
    };
    tcpClientConnection.send(tcpClientPackage, ar -> {
    });

    Assert.assertNull(writeQueue.poll());
    Assert.assertNull(packageQueue.poll());
    Assert.assertEquals(Status.WORKING, Deencapsulation.getField(tcpClientConnection, "status"));
  }

  @Test
  public void connect_success(@Mocked NetSocketImpl netSocket) {
    Promise<NetSocket> promise = Promise.promise();
    new MockUp<NetClientWrapper>(netClientWrapper) {
      @Mock
      void connect(boolean ssl, int port, String host, Handler<AsyncResult<NetSocket>> connectHandler) {
        promise.complete(netSocket);
        connectHandler.handle(promise.future());
      }
    };

    tcpClientConnection.connect();

    Assert.assertSame(netSocket, tcpClientConnection.getNetSocket());
    Assert.assertEquals(Status.WORKING, Deencapsulation.getField(tcpClientConnection, "status"));
  }

  @Test
  public void connect_failed() {
    requestMap.put(10L, new TcpRequest(10, ar -> {
    }));

    Promise<NetSocket> promise = Promise.promise();
    RuntimeException error = new RuntimeExceptionWithoutStackTrace();
    new MockUp<NetClientWrapper>(netClientWrapper) {
      @Mock
      void connect(boolean ssl, int port, String host, Handler<AsyncResult<NetSocket>> connectHandler) {
        promise.fail(error);
        connectHandler.handle(promise.future());
      }
    };

    tcpClientConnection.connect();

    Assert.assertEquals(Status.DISCONNECTED, Deencapsulation.getField(tcpClientConnection, "status"));
    Assert.assertEquals(0, requestMap.size());
  }

  @Test
  public void onClosed(@Mocked NetSocketImpl netSocket) {
    requestMap.put(10L, new TcpRequest(10, ar -> {
    }));
    tcpClientConnection.initNetSocket(netSocket);

    Deencapsulation.invoke(tcpClientConnection, "onClosed", new Class<?>[] {Void.class}, new Object[] {null});
    Assert.assertEquals(Status.DISCONNECTED, Deencapsulation.getField(tcpClientConnection, "status"));
    Assert.assertEquals(0, requestMap.size());
  }

  @Test
  public void onReply_notExist() {
    // should not throw exception
    tcpClientConnection.onReply(1, null, null);
  }

  @Test
  public void on_exist() {
    long msgId = 1L;
    AtomicInteger count = new AtomicInteger();
    requestMap.put(msgId, new TcpRequest(10, ar -> {
      count.incrementAndGet();
    }));

    tcpClientConnection.onReply(msgId, null, null);
    Assert.assertEquals(1, count.get());
  }
}
