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
package org.apache.servicecomb.metrics.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.servicecomb.foundation.metrics.PolledEvent;
import org.apache.servicecomb.foundation.metrics.registry.GlobalRegistry;
import org.apache.servicecomb.foundation.test.scaffolding.config.ArchaiusUtils;
import org.apache.servicecomb.foundation.test.scaffolding.log.LogCollector;
import org.apache.servicecomb.foundation.vertx.SharedVertxFactory;
import org.apache.servicecomb.foundation.vertx.VertxUtils;
import org.apache.servicecomb.foundation.vertx.client.http.HttpClients;
import org.apache.servicecomb.metrics.core.publish.DefaultLogPublisher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.ManualClock;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.api.Meter;
import com.netflix.spectator.api.Registry;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

public class TestVertxMetersInitializer {
  GlobalRegistry globalRegistry = new GlobalRegistry(new ManualClock());

  Registry registry = new DefaultRegistry(globalRegistry.getClock());

  EventBus eventBus = new EventBus();

  VertxMetersInitializer vertxMetersInitializer = new VertxMetersInitializer();

  DefaultLogPublisher logPublisher = new DefaultLogPublisher();

  static int port;

  static String body = "body";

  public static class TestServerVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
      Router mainRouter = Router.router(vertx);
      mainRouter.route("/").handler(context -> {
        context.response().end(context.getBody());
      });

      HttpServer server = vertx.createHttpServer();
      server.requestHandler(mainRouter);
      server.listen(0, "0.0.0.0", ar -> {
        if (ar.succeeded()) {
          port = ar.result().actualPort();
          startPromise.complete();
          return;
        }

        startPromise.fail(ar.cause());
      });
    }
  }

  public static class TestClientVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) {
      HttpClient client = vertx.createHttpClient();
      client.request(HttpMethod.GET, port, "127.0.0.1", "/", ar -> {
        if (ar.succeeded()) {
          HttpClientRequest request = ar.result();
          request.send(resp -> {
            if (resp.succeeded()) {
              resp.result().bodyHandler((buffer) -> {
                startPromise.complete();
              });
            }
          });
          request.end(body);
        }
      });
    }
  }

// TODO: recover this unit tests
//  @Before
//  public void setup() {
//    HttpClients.load();
//  }
//
//  @After
//  public void teardown() {
//    HttpClients.destroy();
//  }
//
//  @Test
//  public void init() throws InterruptedException {
//    globalRegistry.add(registry);
//    vertxMetersInitializer.init(globalRegistry, eventBus, null);
//    logPublisher.init(null, eventBus, null);
//    VertxUtils
//        .blockDeploy(SharedVertxFactory.getSharedVertx(), TestServerVerticle.class, new DeploymentOptions());
//    VertxUtils
//        .blockDeploy(SharedVertxFactory.getSharedVertx(), TestClientVerticle.class, new DeploymentOptions());
//
//    globalRegistry.poll(1);
//    List<Meter> meters = Lists.newArrayList(registry.iterator());
//    List<Measurement> measurements = new ArrayList<>();
//    for (Meter meter : meters) {
//      meter.measure().forEach(measurements::add);
//    }
//
//    LogCollector logCollector = new LogCollector();
//
//    testLog(logCollector, meters, measurements, true);
//    logCollector.clear();
//    testLog(logCollector, meters, measurements, false);
//
//    logCollector.teardown();
//  }
//
//  private void testLog(LogCollector logCollector, List<Meter> meters, List<Measurement> measurements,
//      boolean printDetail) {
//    ArchaiusUtils.setProperty(DefaultLogPublisher.ENDPOINTS_CLIENT_DETAIL_ENABLED, String.valueOf(printDetail));
//    logPublisher.onPolledEvent(new PolledEvent(meters, measurements));
//
//    StringBuilder sb = new StringBuilder();
//    logCollector.getEvents().forEach(event -> sb.append(event.getMessage()).append("\n"));
//    String actual = sb.toString();
//    int idx = actual.indexOf("vertx:\n");
//    actual = actual.substring(idx);
//
//    String clientLatency;
//    String serverLatency;
//
//    String expect = "vertx:\n"
//        + "  instances:\n"
//        + "    name       eventLoopContext-created\n"
//        + "    registry   0\n"
//        + "    registry-watch 0\n"
//        + "    transport  0\n"
//        + "  transport:\n"
//        + "    client.endpoints:\n"
//        + "      connectCount disconnectCount queue         connections requests latency send(Bps) receive(Bps) remote\n";
//
//    int clientLatencyIndex = actual.indexOf("1            0               0             1           1        ")
//        + "1            0               0             1           1        ".length();
//    clientLatency = actual.substring(clientLatencyIndex, actual.indexOf(" ", clientLatencyIndex));
//    int serverLatencyIndex = actual.lastIndexOf("1            0               0             1           1        ")
//        + "1            0               0             1           1        ".length();
//    serverLatency = actual.substring(serverLatencyIndex, actual.indexOf(" ", serverLatencyIndex));
//
//    if (printDetail) {
//      expect +=
//          "      1            0               0             1           1        %-7s 4         21           127.0.0.1:%-5s\n";
//    }
//    expect += ""
//        + "      1            0               0             1           1        %-7s 4         21           (summary)\n"
//        + "    server.endpoints:\n"
//        + "      connectCount disconnectCount rejectByLimit connections requests latency send(Bps) receive(Bps) listen\n"
//        + "      1            0               0             1           1        %-7s 21        4            0.0.0.0:0\n"
//        + "      1            0               0             1           1        %-7s 21        4            (summary)\n\n";
//
//    if (printDetail) {
//      expect = String.format(expect, clientLatency, port, clientLatency, serverLatency, serverLatency);
//    } else {
//      expect = String.format(expect, clientLatency, serverLatency, serverLatency);
//    }
//
//    Assert.assertEquals(expect, actual);
//  }
}
