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
package io.vertx.core.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

public class SyncContext extends EventLoopContext {
  protected VertxInternal owner;

  public SyncContext() {
    this(null);
  }

  public SyncContext(VertxInternal vertx) {
    super(vertx, null, null, null, null, null, null);
  }

  public VertxInternal owner() {
    return owner;
  }

  public void setOwner(VertxInternal owner) {
    this.owner = owner;
  }

  @Override
  public void runOnContext(AbstractContext ctx, Handler<Void> action) {
    action.handle(null);
  }

  public static <T> void syncExecuteBlocking(Handler<Promise<T>> blockingCodeHandler,
      Handler<AsyncResult<T>> asyncResultHandler) {
    Promise<T> res = Promise.promise();

    try {
      blockingCodeHandler.handle(res);
    } catch (Throwable e) {
      res.fail(e);
      return;
    }

    res.future().onComplete(asyncResultHandler);
  }

  private static <T> Future<T> syncExecuteBlocking(Handler<Promise<T>> blockingCodeHandler) {
    Promise<T> res = Promise.promise();

    try {
      blockingCodeHandler.handle(res);
    } catch (Throwable e) {
      res.fail(e);
      return res.future();
    }

    res.complete();
    return res.future();
  }

  @Override
  public <T> Future<T> executeBlockingInternal(Handler<Promise<T>> action) {
    return syncExecuteBlocking(action);
  }

  @Override
  public <T> void executeBlocking(Handler<Promise<T>> blockingCodeHandler, boolean ordered,
      Handler<AsyncResult<T>> asyncResultHandler) {
    syncExecuteBlocking(blockingCodeHandler, asyncResultHandler);
  }
}
