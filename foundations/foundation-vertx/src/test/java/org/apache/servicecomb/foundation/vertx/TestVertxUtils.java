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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.FileUtils;
import org.apache.servicecomb.foundation.common.Holder;
import org.apache.servicecomb.foundation.test.scaffolding.config.ArchaiusUtils;
import org.apache.servicecomb.foundation.vertx.stream.BufferInputStream;
import org.junit.Assert;
import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.impl.FileResolver;

public class TestVertxUtils {
  @Test
  public void testGetOrCreateVertx() throws InterruptedException {
    Vertx vertx = VertxUtils.getOrCreateVertxByName("ut", null);

    Holder<String> name = new Holder<>();
    CountDownLatch latch = new CountDownLatch(1);
    vertx.runOnContext(v -> {
      name.value = Thread.currentThread().getName();
      latch.countDown();
    });
    latch.await();

    Assert.assertEquals(name.value, "ut-vert.x-eventloop-thread-0");
    VertxUtils.blockCloseVertxByName("ut");
  }

  @Test
  public void testCreateVertxWithFileCPResolving() {
    // Prepare
    ArchaiusUtils.resetConfig();

    // create .vertx folder
    ArchaiusUtils.setProperty(FileResolver.DISABLE_CP_RESOLVING_PROP_NAME, false);
    deleteCacheFile();
    VertxUtils.getOrCreateVertxByName("testCreateVertxWithFileCPResolvingFalse", null);
    Assert.assertTrue(isCacheFileExists());
    VertxUtils.blockCloseVertxByName("testCreateVertxWithFileCPResolvingFalse");

    // don't create .vertx folder
    deleteCacheFile();
    Assert.assertFalse(isCacheFileExists());
    ArchaiusUtils.setProperty(FileResolver.DISABLE_CP_RESOLVING_PROP_NAME, true);
    VertxUtils.getOrCreateVertxByName("testCreateVertxWithFileCPResolvingTrue", null);
    Assert.assertFalse(isCacheFileExists());
    VertxUtils.blockCloseVertxByName("testCreateVertxWithFileCPResolvingTrue");

    ArchaiusUtils.resetConfig();
  }

  private void deleteCacheFile() {
    String cacheDirBase = System.getProperty(FileResolver.CACHE_DIR_BASE_PROP_NAME,
        System.getProperty("java.io.tmpdir", "."));
    File folder = new File(cacheDirBase);
    File[] files = folder.listFiles();
    for (File f : files) {
      if (f.getName().startsWith("vertx-cache")) {
        FileUtils.deleteQuietly(f);
      }
    }
  }

  private boolean isCacheFileExists() {
    String cacheDirBase = System.getProperty(FileResolver.CACHE_DIR_BASE_PROP_NAME,
        System.getProperty("java.io.tmpdir", "."));
    File folder = new File(cacheDirBase);
    File[] files = folder.listFiles();
    for (File f : files) {
      if (f.getName().startsWith("vertx-cache")) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void testVertxUtilsInitNullOptions() {
    Vertx vertx = VertxUtils.init(null, null);
    Assert.assertNotEquals(null, vertx);
    VertxUtils.blockCloseVertx(vertx);
  }

  @Test
  public void testVertxUtilsInitWithOptions() {
    VertxOptions oOptions = new VertxOptions();

    Vertx vertx = VertxUtils.init(null, oOptions);
    Assert.assertNotEquals(null, vertx);
    VertxUtils.blockCloseVertx(vertx);
  }

  @Test
  public void testgetBytesFastBufferInputStream() throws IOException {
    byte[] bytes = new byte[] {1};
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);

    try (BufferInputStream inputStream = new BufferInputStream(byteBuf)) {
      byte[] result = VertxUtils.getBytesFast(inputStream);
      Assert.assertSame(bytes, result);
    }
  }

  @Test
  public void testgetBytesFastNormalInputStream() throws IOException {
    byte[] bytes = new byte[] {1};

    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
      byte[] result = VertxUtils.getBytesFast(inputStream);
      Assert.assertEquals(1, result[0]);
    }
  }

  @Test
  public void testgetBytesFastBuffer() {
    Buffer buffer = Buffer.buffer();
    buffer.appendByte((byte) 1);

    byte[] result = VertxUtils.getBytesFast(buffer);
    Assert.assertEquals(1, result[0]);
  }

  @Test
  public void testgetBytesFastByteBufHasArray() {
    byte[] bytes = new byte[] {1};
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);

    byte[] result = VertxUtils.getBytesFast(byteBuf);
    Assert.assertSame(bytes, result);
  }

  @Test
  public void testgetBytesFastByteBufCopy() {
    ByteBuf byteBuf = Unpooled.directBuffer();
    byteBuf.writeByte(1);
    Assert.assertFalse(byteBuf.hasArray());

    byte[] result = VertxUtils.getBytesFast(byteBuf);
    Assert.assertEquals(1, result[0]);

    byteBuf.release();
  }
}
