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
package org.apache.servicecomb.foundation.vertx.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.servicecomb.foundation.common.io.AsyncCloseable;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

/**
 * for pump from a readStream
 */
public class OutputStreamToWriteStream implements WriteStream<Buffer>, AsyncCloseable<Void> {
  private static final int DEFAULT_MAX_BUFFERS = 4;

  private static final int SMALLEST_MAX_BUFFERS = 2;

  private OutputStream outputStream;

  private Context context;

  private boolean autoCloseOutputStream;

  private Handler<Throwable> exceptionHandler;

  // resume readStream
  private Handler<Void> drainHandler;

  // when invoke close, but outputStream not write all data, must put close logic to closedDeferred
  private Runnable closedDeferred;

  private boolean closed;

  // buffers.size() need to loop all node, and maybe result is not correct in concurrent condition
  // we just need to flow control by pump, so use another size
  private Queue<Buffer> buffers = new ConcurrentLinkedQueue<>();

  private int currentBufferCount;

  // just indicate if buffers is full, not control add logic
  // must >= SMALLEST_MAX_BUFFERS
  // if < SMALLEST_MAX_BUFFERS, then maxBuffers will be SMALLEST_MAX_BUFFERS
  private int maxBuffers = DEFAULT_MAX_BUFFERS;

  // if currentBufferCount <= drainMark, will invoke drainHandler to resume readStream
  private int drainMark = maxBuffers / 2;

  public OutputStreamToWriteStream(Context context, OutputStream outputStream,
      boolean autoCloseOutputStream) {
    this.context = context;
    this.outputStream = outputStream;
    this.autoCloseOutputStream = autoCloseOutputStream;
  }

  @Override
  public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  private void handleException(Throwable t) {
    if (exceptionHandler != null) {
      exceptionHandler.handle(t);
    }
  }

  @Override
  public synchronized Future<Void> write(Buffer data) {
    Promise<Void> result = Promise.<Void>promise();
    write(data, ar -> {
      if (ar.failed()) {
        handleException(ar.cause());
      }
      result.complete();
    });
    return result.future();
  }

  @Override
  public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
    currentBufferCount++;
    buffers.add(data);
    context.executeBlocking(this::writeInWorker,
        true,
        handler);
  }

  protected void writeInWorker(Promise<Void> future) {
    while (true) {
      Buffer buffer = buffers.poll();
      if (buffer == null) {
        future.complete();
        return;
      }

      try {
        outputStream.write(buffer.getBytes());

        synchronized (OutputStreamToWriteStream.this) {
          currentBufferCount--;
          Runnable action = (currentBufferCount == 0 && closedDeferred != null) ? closedDeferred : this::checkDrained;
          action.run();
        }
      } catch (IOException e) {
        currentBufferCount--;
        future.fail(e);
        return;
      }
    }
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    close();
  }

  @Override
  public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
    this.maxBuffers = maxSize < SMALLEST_MAX_BUFFERS ? SMALLEST_MAX_BUFFERS : maxSize;
    this.drainMark = maxBuffers / 2;
    return this;
  }

  @Override
  public synchronized boolean writeQueueFull() {
    return currentBufferCount >= maxBuffers;
  }

  @Override
  public synchronized WriteStream<Buffer> drainHandler(Handler<Void> handler) {
    this.drainHandler = handler;
    return this;
  }

  private synchronized void checkDrained() {
    if (drainHandler != null && currentBufferCount <= drainMark) {
      Handler<Void> handler = drainHandler;
      drainHandler = null;
      handler.handle(null);
    }
  }

  @Override
  public CompletableFuture<Void> close() {
    return closeInternal();
  }

  private void check() {
    checkClosed();
  }

  private void checkClosed() {
    if (closed) {
      throw new IllegalStateException(this.getClass().getName() + " is closed");
    }
  }

  private synchronized CompletableFuture<Void> closeInternal() {
    check();

    closed = true;

    CompletableFuture<Void> future = new CompletableFuture<>();
    if (currentBufferCount == 0) {
      doClose(future);
    } else {
      closedDeferred = () -> doClose(future);
    }
    return future;
  }

  private void doClose(CompletableFuture<Void> future) {
    if (autoCloseOutputStream) {
      try {
        outputStream.close();
      } catch (IOException e) {
        future.completeExceptionally(new IllegalStateException("failed to close outputStream.", e));
        return;
      }
    }

    future.complete(null);
  }
}
