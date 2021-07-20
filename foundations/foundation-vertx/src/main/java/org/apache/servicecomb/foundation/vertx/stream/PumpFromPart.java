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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.apache.servicecomb.foundation.vertx.http.DownloadUtils;
import org.apache.servicecomb.foundation.vertx.http.ReadStreamPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

public class PumpFromPart {
  private static final Logger LOGGER = LoggerFactory.getLogger(PumpFromPart.class);

  private Context context;

  private Part part;

  public PumpFromPart(Context context, Part part) {
    this.context = context;
    this.part = part;
  }

  private CompletableFuture<ReadStream<Buffer>> prepareReadStream() {
    CompletableFuture<ReadStream<Buffer>> future = new CompletableFuture<>();

    if (ReadStreamPart.class.isInstance(part)) {
      future.complete(((ReadStreamPart) part).getReadStream());
      return future;
    }

    try {
      InputStream inputStream = part.getInputStream();
      InputStreamToReadStream inputStreamToReadStream = new InputStreamToReadStream(context, inputStream, true);
      inputStreamToReadStream.pause();
      future.complete(inputStreamToReadStream);
    } catch (IOException e) {
      future.completeExceptionally(e);
    }

    return future;
  }

  public CompletableFuture<Void> toWriteStream(WriteStream<Buffer> writeStream, Handler<Throwable> throwableHandler) {
    return prepareReadStream()
        .thenCompose(readStream -> new PumpCommon().pump(context, readStream, writeStream, throwableHandler))
        .whenComplete((v, e) -> {
          if (e != null) {
            LOGGER.error("to write stream failed.", e);
          }
          DownloadUtils.clearPartResource(part);
          // PumpImpl will add drainHandler to writeStream,
          // in order to support write multiple files to same writeStream,
          // need reset after one stream is successful.
          writeStream.drainHandler(null);
        });
  }

  public CompletableFuture<Void> toOutputStream(OutputStream outputStream, boolean autoCloseOutputStream) {
    if (context == null) {
      return toOutputStreamSync(outputStream, autoCloseOutputStream);
    }

    return toOutputStreamAsync(outputStream, autoCloseOutputStream);
  }

  private CompletableFuture<Void> toOutputStreamAsync(OutputStream outputStream, boolean autoCloseOutputStream) {
    OutputStreamToWriteStream outputStreamToWriteStream = new OutputStreamToWriteStream(context, outputStream,
        autoCloseOutputStream);
    return toWriteStream(outputStreamToWriteStream, null);
  }

  // DO NOT use a mocked sync context to unify the pump logic
  // otherwise when pump big stream, will cause stack overflow
  private CompletableFuture<Void> toOutputStreamSync(OutputStream outputStream, boolean autoCloseOutputStream) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.whenComplete((v, e) -> DownloadUtils.clearPartResource(part));

    try (InputStream inputStream = part.getInputStream()) {
      IOUtils.copyLarge(inputStream, outputStream);
    } catch (Throwable e) {
      future.completeExceptionally(e);
    }

    if (autoCloseOutputStream) {
      try {
        outputStream.close();
      } catch (Throwable e) {
        future.completeExceptionally(e);
      }
    }

    future.complete(null);
    return future;
  }
}
