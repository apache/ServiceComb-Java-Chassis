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

package org.apache.servicecomb.foundation.vertx;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.servicecomb.foundation.common.Holder;
import org.apache.servicecomb.foundation.common.concurrent.ConcurrentHashMapEx;
import org.apache.servicecomb.foundation.vertx.client.ClientPoolManager;
import org.apache.servicecomb.foundation.vertx.client.ClientVerticle;
import org.apache.servicecomb.foundation.vertx.stream.BufferInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicPropertyFactory;

import io.netty.buffer.ByteBuf;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.impl.FileResolver;
import io.vertx.core.impl.VertxBuilder;
import io.vertx.core.impl.VertxThread;
import io.vertx.core.spi.VertxThreadFactory;

/**
 * VertxUtils
 *
 *
 */
public final class VertxUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(VertxUtils.class);

  private static final long BLOCKED_THREAD_CHECK_INTERVAL = Long.MAX_VALUE / 2;

  // key为vertx实例名称，以支撑vertx功能分组
  private static Map<String, Vertx> vertxMap = new ConcurrentHashMapEx<>();

  private VertxUtils() {
  }

  public static Map<String, Vertx> getVertxMap() {
    return vertxMap;
  }

  public static <T extends AbstractVerticle> void deployVerticle(Vertx vertx, Class<T> cls, int instanceCount) {
    DeploymentOptions options = new DeploymentOptions().setInstances(instanceCount);
    vertx.deployVerticle(cls.getName(), options);
  }

  public static <CLIENT_POOL> DeploymentOptions createClientDeployOptions(
      ClientPoolManager<CLIENT_POOL> clientMgr,
      int instanceCount) {
    DeploymentOptions options = new DeploymentOptions().setInstances(instanceCount);
    SimpleJsonObject config = new SimpleJsonObject();
    config.put(ClientVerticle.CLIENT_MGR, clientMgr);
    options.setConfig(config);

    return options;
  }

  // deploy Verticle and wait for its success. do not call this method in event-loop thread
  public static <VERTICLE extends Verticle> boolean blockDeploy(Vertx vertx,
      Class<VERTICLE> cls,
      DeploymentOptions options) throws InterruptedException {
    Holder<Boolean> result = new Holder<>();

    CountDownLatch latch = new CountDownLatch(1);
    vertx.deployVerticle(cls.getName(), options, ar -> {
      result.value = ar.succeeded();

      if (ar.failed()) {
        LOGGER.error("deploy vertx failed, cause ", ar.cause());
      }

      latch.countDown();
    });

    latch.await();

    return result.value;
  }

  public static Vertx getOrCreateVertxByName(String name, VertxOptions vertxOptions) {
    return vertxMap.computeIfAbsent(name, vertxName -> init(vertxName, vertxOptions));
  }

  public static Vertx init(String name, VertxOptions vertxOptions) {
    if (vertxOptions == null) {
      vertxOptions = new VertxOptions();
    }

    boolean isDebug = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("jdwp");
    if (isDebug) {
      vertxOptions.setBlockedThreadCheckInterval(BLOCKED_THREAD_CHECK_INTERVAL);
      LOGGER.info("in debug mode, disable blocked thread check.");
    }

    configureVertxFileCaching(vertxOptions);

    return new VertxBuilder(vertxOptions).threadFactory(new VertxThreadFactory() {
      @Override
      public VertxThread newVertxThread(Runnable target, String threadName, boolean worker, long maxExecTime,
          TimeUnit maxExecTimeUnit) {
        return VertxThreadFactory.super
            .newVertxThread(target, name + "-" + threadName, worker, maxExecTime, maxExecTimeUnit);
      }
    }).init().vertx();
  }

  /**
   * 配置vertx的文件缓存功能，默认关闭
   */
  protected static void configureVertxFileCaching(VertxOptions vertxOptions) {
    boolean disableFileCPResolving = DynamicPropertyFactory.getInstance()
        .getBooleanProperty(FileResolver.DISABLE_CP_RESOLVING_PROP_NAME, true).get();
    vertxOptions.getFileSystemOptions().setClassPathResolvingEnabled(!disableFileCPResolving);
  }

  // try to reference byte[]
  // otherwise copy byte[]
  public static byte[] getBytesFast(InputStream inputStream) throws IOException {
    if (BufferInputStream.class.isInstance(inputStream)) {
      return getBytesFast(((BufferInputStream) inputStream).getByteBuf());
    }

    return IOUtils.toByteArray(inputStream);
  }

  public static byte[] getBytesFast(Buffer buffer) {
    ByteBuf byteBuf = buffer.getByteBuf();
    return getBytesFast(byteBuf);
  }

  public static byte[] getBytesFast(ByteBuf byteBuf) {
    if (byteBuf.hasArray()) {
      return byteBuf.array();
    }

    byte[] arr = new byte[byteBuf.writerIndex()];
    byteBuf.getBytes(0, arr);
    return arr;
  }

  public static CompletableFuture<Void> closeVertxByName(String name) {
    LOGGER.info("Closing vertx {}.", name);
    CompletableFuture<Void> future = new CompletableFuture<>();
    Vertx vertx = vertxMap.remove(name);
    if (vertx == null) {
      LOGGER.info("Vertx {} not exist.", name);
      future.complete(null);
      return future;
    }

    vertx.close(ar -> {
      if (ar.succeeded()) {
        LOGGER.info("Success to close vertx {}.", name);
        future.complete(null);
        return;
      }

      future.completeExceptionally(ar.cause());
    });
    return future;
  }

  public static void blockCloseVertxByName(String name) {
    CompletableFuture<Void> future = closeVertxByName(name);
    try {
      future.get();
    } catch (Throwable e) {
      LOGGER.error("Failed to wait close vertx {}.", name, e);
    }
  }

  public static void blockCloseVertx(Vertx vertx) {
    CountDownLatch latch = new CountDownLatch(1);
    vertx.close(ar -> {
      if (ar.succeeded()) {
        LOGGER.info("Success to close vertx {}.", vertx);
      } else {
        LOGGER.info("Failed to close vertx {}.", vertx);
      }

      latch.countDown();
    });

    try {
      latch.await();
    } catch (InterruptedException e) {
      LOGGER.info("Failed to wait close vertx {}.", vertx);
    }
  }
}
