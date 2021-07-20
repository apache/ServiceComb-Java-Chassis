/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 *
 */

/*
 *  Froked from https://github.com/vert-x3/vertx-web/blob/master/vertx-web/src/main/java/io/vertx/ext/web/handler/impl/BodyHandlerImpl.java
 *
 */
package org.apache.servicecomb.transport.rest.vertx;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.servicecomb.swagger.invocation.exception.CommonExceptionData;
import org.apache.servicecomb.swagger.invocation.exception.ExceptionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;
import io.vertx.ext.web.impl.FileUploadImpl;
import io.vertx.ext.web.impl.RoutingContextInternal;

/**
 * copy from io.vertx.ext.web.handler.impl.BodyHandlerImpl
 * and modified.
 *
 * allowed to disable fileupload by setUploadsDirectory(null)
 */
public class RestBodyHandler implements BodyHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(BodyHandlerImpl.class);

  private long bodyLimit = DEFAULT_BODY_LIMIT;

  private boolean handleFileUploads;

  private String uploadsDir;

  private boolean mergeFormAttributes = DEFAULT_MERGE_FORM_ATTRIBUTES;

  private boolean deleteUploadedFilesOnEnd = DEFAULT_DELETE_UPLOADED_FILES_ON_END;

  private boolean isPreallocateBodyBuffer = DEFAULT_PREALLOCATE_BODY_BUFFER;

  private static final int DEFAULT_INITIAL_BODY_BUFFER_SIZE = 1024; //bytes

  public static final String BYPASS_BODY_HANDLER = "__bypass_body_handler";

  public RestBodyHandler() {
    this(true, DEFAULT_UPLOADS_DIRECTORY);
  }

  public RestBodyHandler(boolean handleFileUploads) {
    this(handleFileUploads, DEFAULT_UPLOADS_DIRECTORY);
  }

  public RestBodyHandler(String uploadDirectory) {
    this(true, uploadDirectory);
  }

  private RestBodyHandler(boolean handleFileUploads, String uploadDirectory) {
    this.handleFileUploads = handleFileUploads;
    setUploadsDirectory(uploadDirectory);
  }

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    if (request.headers().contains(HttpHeaders.UPGRADE, HttpHeaders.WEBSOCKET, true)) {
      context.next();
      return;
    }

    Boolean bypass = context.get(BYPASS_BODY_HANDLER);
    if (Boolean.TRUE.equals(bypass)) {
      context.next();
      return;
    }

    // we need to keep state since we can be called again on reroute
    if (!((RoutingContextInternal) context).seenHandler(RoutingContextInternal.BODY_HANDLER)) {
      long contentLength = isPreallocateBodyBuffer ? parseContentLengthHeader(request) : -1;
      BHandler handler = new BHandler(context, contentLength);
      request.handler(handler);
      request.endHandler(v -> handler.end());
      ((RoutingContextInternal) context).visitHandler(RoutingContextInternal.BODY_HANDLER);
    } else {
      // on reroute we need to re-merge the form params if that was desired
      if (mergeFormAttributes && request.isExpectMultipart()) {
        request.params().addAll(request.formAttributes());
      }

      context.next();
    }
  }

  @Override
  public BodyHandler setHandleFileUploads(boolean handleFileUploads) {
    this.handleFileUploads = handleFileUploads;
    return this;
  }

  @Override
  public BodyHandler setBodyLimit(long bodyLimit) {
    this.bodyLimit = bodyLimit;
    return this;
  }

  @Override
  public BodyHandler setUploadsDirectory(String uploadsDirectory) {
    this.uploadsDir = uploadsDirectory;
    return this;
  }

  @Override
  public BodyHandler setMergeFormAttributes(boolean mergeFormAttributes) {
    this.mergeFormAttributes = mergeFormAttributes;
    return this;
  }

  @Override
  public BodyHandler setDeleteUploadedFilesOnEnd(boolean deleteUploadedFilesOnEnd) {
    this.deleteUploadedFilesOnEnd = deleteUploadedFilesOnEnd;
    return this;
  }

  @Override
  public BodyHandler setPreallocateBodyBuffer(boolean isPreallocateBodyBuffer) {
    this.isPreallocateBodyBuffer = isPreallocateBodyBuffer;
    return this;
  }

  private long parseContentLengthHeader(HttpServerRequest request) {
    String contentLength = request.getHeader(HttpHeaders.CONTENT_LENGTH);
    if (contentLength == null || contentLength.isEmpty()) {
      return -1;
    }
    try {
      long parsedContentLength = Long.parseLong(contentLength);
      return parsedContentLength < 0 ? -1 : parsedContentLength;
    } catch (NumberFormatException ex) {
      return -1;
    }
  }

  private class BHandler implements Handler<Buffer> {
    private static final int MAX_PREALLOCATED_BODY_BUFFER_BYTES = 65535;

    final RoutingContext context;

    final long contentLength;

    Buffer body;

    boolean failed;

    AtomicInteger uploadCount = new AtomicInteger();

    AtomicBoolean cleanup = new AtomicBoolean(false);

    boolean ended;

    long uploadSize = 0L;

    final boolean isMultipart;

    final boolean isUrlEncoded;

    public BHandler(RoutingContext context, long contentLength) {
      this.context = context;
      this.contentLength = contentLength;
      // the request clearly states that there should
      // be a body, so we respect the client and ensure
      // that the body will not be null
      if (contentLength != -1) {
        initBodyBuffer();
      }

      Set<FileUpload> fileUploads = context.fileUploads();

      final String contentType = context.request().getHeader(HttpHeaders.CONTENT_TYPE);
      if (contentType == null) {
        isMultipart = false;
        isUrlEncoded = false;
      } else {
        final String lowerCaseContentType = contentType.toLowerCase();
        isMultipart = lowerCaseContentType.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString());
        isUrlEncoded = lowerCaseContentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString());
      }

      if (isMultipart || isUrlEncoded) {
        context.request().setExpectMultipart(true);
        if (handleFileUploads) {
          makeUploadDir(context.vertx().fileSystem());
        }
        context.request().uploadHandler(upload -> {
          // *** cse begin ***
          if (uploadsDir == null) {
            failed = true;
            CommonExceptionData data = new CommonExceptionData("not support file upload.");
            context.fail(ExceptionFactory.createProducerException(data));
            return;
          }
          // *** cse end ***
          if (bodyLimit != -1 && upload.isSizeAvailable()) {
            // we can try to abort even before the upload starts
            long size = uploadSize + upload.size();
            if (size > bodyLimit) {
              failed = true;
              cancelAndCleanupFileUploads();
              context.fail(413);
              return;
            }
          }
          if (handleFileUploads) {
            // we actually upload to a file with a generated filename
            uploadCount.incrementAndGet();
            String uploadedFileName = new File(uploadsDir, UUID.randomUUID().toString()).getPath();
            FileUploadImpl fileUpload = new FileUploadImpl(uploadedFileName, upload);
            fileUploads.add(fileUpload);
            Future<Void> fut = upload.streamToFileSystem(uploadedFileName);
            fut.onComplete(ar -> {
              if (fut.succeeded()) {
                uploadEnded();
              } else {
                cancelAndCleanupFileUploads();
                context.fail(ar.cause());
              }
            });
          }
        });
      }

      context.request().exceptionHandler(t -> {
        cancelAndCleanupFileUploads();
        if (t instanceof DecoderException) {
          // bad request
          context.fail(400, t.getCause());
        } else {
          context.fail(t);
        }
      });
    }

    private void initBodyBuffer() {
      int initialBodyBufferSize;
      if (contentLength < 0) {
        initialBodyBufferSize = DEFAULT_INITIAL_BODY_BUFFER_SIZE;
      } else if (contentLength > MAX_PREALLOCATED_BODY_BUFFER_BYTES) {
        initialBodyBufferSize = MAX_PREALLOCATED_BODY_BUFFER_BYTES;
      } else {
        initialBodyBufferSize = (int) contentLength;
      }

      if (bodyLimit != -1) {
        initialBodyBufferSize = (int) Math.min(initialBodyBufferSize, bodyLimit);
      }

      this.body = Buffer.buffer(initialBodyBufferSize);
    }

    private void makeUploadDir(FileSystem fileSystem) {
      // *** cse begin ***
      if (uploadsDir == null) {
        return;
      }
      // *** cse end ***

      if (!fileSystem.existsBlocking(uploadsDir)) {
        fileSystem.mkdirsBlocking(uploadsDir);
      }
    }

    @Override
    public void handle(Buffer buff) {
      if (failed) {
        return;
      }
      uploadSize += buff.length();
      if (bodyLimit != -1 && uploadSize > bodyLimit) {
        failed = true;
        cancelAndCleanupFileUploads();
        context.fail(413);
      } else {
        // multipart requests will not end up in the request body
        // url encoded should also not, however jQuery by default
        // post in urlencoded even if the payload is something else
        if (!isMultipart /* && !isUrlEncoded */) {
          if (body == null) {
            initBodyBuffer();
          }
          body.appendBuffer(buff);
        }
      }
    }

    void uploadEnded() {
      int count = uploadCount.decrementAndGet();
      // only if parsing is done and count is 0 then all files have been processed
      if (ended && count == 0) {
        doEnd();
      }
    }

    void end() {
      // this marks the end of body parsing, calling doEnd should
      // only be possible from this moment onwards
      ended = true;

      // only if parsing is done and count is 0 then all files have been processed
      if (uploadCount.get() == 0) {
        doEnd();
      }
    }

    void doEnd() {

      if (failed) {
        cancelAndCleanupFileUploads();
        return;
      }

      if (deleteUploadedFilesOnEnd) {
        context.addBodyEndHandler(x -> cancelAndCleanupFileUploads());
      }

      HttpServerRequest req = context.request();
      if (mergeFormAttributes && req.isExpectMultipart()) {
        req.params().addAll(req.formAttributes());
      }
      context.setBody(body);
      // release body as it may take lots of memory
      body = null;

      context.next();
    }

    /**
     * Cancel all unfinished file upload in progress and delete all uploaded files.
     */
    private void cancelAndCleanupFileUploads() {
      if (cleanup.compareAndSet(false, true) && handleFileUploads) {
        for (FileUpload fileUpload : context.fileUploads()) {
          FileSystem fileSystem = context.vertx().fileSystem();
          if (!fileUpload.cancel()) {
            String uploadedFileName = fileUpload.uploadedFileName();
            fileSystem.delete(uploadedFileName, deleteResult -> {
              if (deleteResult.failed()) {
                LOGGER.warn("Delete of uploaded file failed: " + uploadedFileName, deleteResult.cause());
              }
            });
          }
        }
      }
    }
  }
}
