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

package org.apache.servicecomb.transport.rest.client.http;

import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.Response.Status;

import org.apache.servicecomb.common.rest.RestConst;
import org.apache.servicecomb.common.rest.codec.param.RestClientRequestImpl;
import org.apache.servicecomb.common.rest.definition.RestOperationMeta;
import org.apache.servicecomb.common.rest.filter.HttpClientFilter;
import org.apache.servicecomb.core.Const;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.core.definition.OperationConfig;
import org.apache.servicecomb.core.definition.OperationMeta;
import org.apache.servicecomb.core.invocation.InvocationStageTrace;
import org.apache.servicecomb.foundation.common.http.HttpStatus;
import org.apache.servicecomb.foundation.common.net.IpPort;
import org.apache.servicecomb.foundation.common.net.URIEndpointObject;
import org.apache.servicecomb.foundation.common.utils.ExceptionUtils;
import org.apache.servicecomb.foundation.common.utils.JsonUtils;
import org.apache.servicecomb.foundation.common.utils.SPIServiceUtils;
import org.apache.servicecomb.foundation.vertx.client.http.HttpClientWithContext;
import org.apache.servicecomb.foundation.vertx.http.HttpServletRequestEx;
import org.apache.servicecomb.foundation.vertx.http.HttpServletResponseEx;
import org.apache.servicecomb.foundation.vertx.http.ReadStreamPart;
import org.apache.servicecomb.foundation.vertx.http.VertxClientRequestToHttpServletRequest;
import org.apache.servicecomb.foundation.vertx.http.VertxClientResponseToHttpServletResponse;
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultHttpSocketMetric;
import org.apache.servicecomb.registry.definition.DefinitionConst;
import org.apache.servicecomb.swagger.invocation.AsyncResponse;
import org.apache.servicecomb.swagger.invocation.Response;
import org.apache.servicecomb.swagger.invocation.exception.CommonExceptionData;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.apache.servicecomb.transport.rest.client.HttpRestClientInvocationHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.ConnectionBase;

public class RestClientInvocation {
  private static final Logger LOGGER = LoggerFactory.getLogger(RestClientInvocation.class);

  private static final String[] INTERNAL_HEADERS = new String[] {
      org.apache.servicecomb.core.Const.CSE_CONTEXT,
      org.apache.servicecomb.core.Const.TARGET_MICROSERVICE
  };

  private HttpClientWithContext httpClientWithContext;

  private Invocation invocation;

  private RestOperationMeta restOperationMeta;

  private AsyncResponse asyncResp;

  private List<HttpClientFilter> httpClientFilters;

  private HttpClientRequest clientRequest;

  private HttpClientResponse clientResponse;

  private Handler<Throwable> throwableHandler = e -> fail((ConnectionBase) clientRequest.connection(), e);

  private boolean alreadyFailed = false;

  public RestClientInvocation(HttpClientWithContext httpClientWithContext, List<HttpClientFilter> httpClientFilters) {
    this.httpClientWithContext = httpClientWithContext;
    this.httpClientFilters = httpClientFilters;
  }

  public void invoke(Invocation invocation, AsyncResponse asyncResp) throws Exception {
    this.invocation = invocation;
    this.asyncResp = asyncResp;

    OperationMeta operationMeta = invocation.getOperationMeta();
    restOperationMeta = operationMeta.getExtData(RestConst.SWAGGER_REST_OPERATION);

    String path = this.createRequestPath(restOperationMeta);
    IpPort ipPort = (IpPort) invocation.getEndpoint().getAddress();

    createRequest(ipPort, path);
    clientRequest.putHeader(org.apache.servicecomb.core.Const.TARGET_MICROSERVICE, invocation.getMicroserviceName());
    RestClientRequestImpl restClientRequest =
        new RestClientRequestImpl(clientRequest, httpClientWithContext.context(), asyncResp, throwableHandler);
    invocation.getHandlerContext().put(RestConst.INVOCATION_HANDLER_REQUESTCLIENT, restClientRequest);

    Buffer requestBodyBuffer = restClientRequest.getBodyBuffer();
    HttpServletRequestEx requestEx = new VertxClientRequestToHttpServletRequest(clientRequest, requestBodyBuffer);
    invocation.getInvocationStageTrace().startClientFiltersRequest();
    for (HttpClientFilter filter : httpClientFilters) {
      if (filter.enabled()) {
        filter.beforeSendRequest(invocation, requestEx);
      }
    }
    List<HttpRestClientInvocationHook> httpRestClientInvocationHooks =
            SPIServiceUtils.getAllService(HttpRestClientInvocationHook.class);

    clientRequest.exceptionHandler(e -> {
      invocation.getTraceIdLogger()
          .error(LOGGER, "Failed to send request, alreadyFailed:{}, local:{}, remote:{}, message={}.",
              alreadyFailed, getLocalAddress(), ipPort.getSocketAddress(),
              ExceptionUtils.getExceptionMessageWithoutTrace(e));
      httpRestClientInvocationHooks.forEach(httpServerExceptionHandler -> {
        httpServerExceptionHandler.handle(e);
      });
      throwableHandler.handle(e);
    });

    // 从业务线程转移到网络线程中去发送
    invocation.onStartSendRequest();
    httpClientWithContext.runOnContext(httpClient -> {
      clientRequest.setTimeout(operationMeta.getConfig().getMsRequestTimeout());
      processServiceCombHeaders(invocation, operationMeta);
      try {
        restClientRequest.end();
      } catch (Throwable e) {
        invocation.getTraceIdLogger().error(LOGGER,
            "send http request failed, alreadyFailed:{}, local:{}, remote: {}, message={}.",
            alreadyFailed,
            getLocalAddress(), ipPort
            , ExceptionUtils.getExceptionMessageWithoutTrace(e));
        fail((ConnectionBase) clientRequest.connection(), e);
      }
    });
  }

  /**
   * If this is a 3rd party invocation, ServiceComb related headers should be removed by default to hide inner
   * implementation. Otherwise, the InvocationContext will be set into the request headers.
   *
   * @see OperationConfig#isClientRequestHeaderFilterEnabled()
   * @param invocation  invocation determines whether this is an invocation to 3rd party services
   * @param operationMeta operationMeta determines whether to remove certain headers and which headers should be removed
   */
  private void processServiceCombHeaders(Invocation invocation, OperationMeta operationMeta) {
    if (invocation.isThirdPartyInvocation() && operationMeta.getConfig().isClientRequestHeaderFilterEnabled()) {
      for (String internalHeaderName : INTERNAL_HEADERS) {
        clientRequest.headers().remove(internalHeaderName);
      }
      return;
    }
    this.setCseContext();
  }

  private String getLocalAddress() {
    HttpConnection connection = clientRequest.connection();
    if (connection == null) {
      return "not connected";
    }
    SocketAddress socketAddress = connection.localAddress();
    return socketAddress != null ? socketAddress.toString() : "not connected";
  }

  private HttpMethod getMethod() {
    OperationMeta operationMeta = invocation.getOperationMeta();
    RestOperationMeta swaggerRestOperation = operationMeta.getExtData(RestConst.SWAGGER_REST_OPERATION);
    String method = swaggerRestOperation.getHttpMethod();
    return HttpMethod.valueOf(method);
  }

  @SuppressWarnings("deprecation")
  void createRequest(IpPort ipPort, String path) {
    URIEndpointObject endpoint = (URIEndpointObject) invocation.getEndpoint().getAddress();
    RequestOptions requestOptions = new RequestOptions();
    requestOptions.setHost(ipPort.getHostOrIp())
        .setPort(ipPort.getPort())
        .setSsl(endpoint.isSslEnabled())
        .setURI(path);

    HttpMethod method = getMethod();
    invocation.getTraceIdLogger()
        .debug(LOGGER, "Sending request by rest, method={}, qualifiedName={}, path={}, endpoint={}.",
            method,
            invocation.getMicroserviceQualifiedName(),
            path,
            invocation.getEndpoint().getEndpoint());
    clientRequest = httpClientWithContext.getHttpClient().request(method, requestOptions, this::handleResponse);
  }

  protected void handleResponse(HttpClientResponse httpClientResponse) {
    this.clientResponse = httpClientResponse;

    if (HttpStatus.isSuccess(clientResponse.statusCode()) && restOperationMeta.isDownloadFile()) {
      ReadStreamPart part = new ReadStreamPart(httpClientWithContext.context(), httpClientResponse);
      invocation.getHandlerContext().put(RestConst.READ_STREAM_PART, part);
      processResponseBody(null);
      return;
    }

    httpClientResponse.exceptionHandler(e -> {
      invocation.getTraceIdLogger().error(LOGGER, "Failed to receive response, local:{}, remote:{}, message={}.",
          getLocalAddress(), httpClientResponse.netSocket().remoteAddress(),
          ExceptionUtils.getExceptionMessageWithoutTrace(e));
      fail((ConnectionBase) clientRequest.connection(), e);
    });

    clientResponse.bodyHandler(this::processResponseBody);
  }

  /**
   * after this method, connection will be recycled to connection pool
   * @param responseBuf response body buffer, when download, responseBuf is null, because download data by ReadStreamPart
   */
  protected void processResponseBody(Buffer responseBuf) {
    DefaultHttpSocketMetric httpSocketMetric = (DefaultHttpSocketMetric) ((ConnectionBase) clientRequest.connection())
        .metric();
    invocation.getInvocationStageTrace().finishGetConnection(httpSocketMetric.getRequestBeginTime());
    invocation.getInvocationStageTrace().finishWriteToBuffer(httpSocketMetric.getRequestEndTime());
    invocation.getInvocationStageTrace().finishReceiveResponse();

    invocation.getResponseExecutor().execute(() -> {
      try {
        invocation.getInvocationStageTrace().startClientFiltersResponse();
        HttpServletResponseEx responseEx =
            new VertxClientResponseToHttpServletResponse(clientResponse, responseBuf);
        for (HttpClientFilter filter : httpClientFilters) {
          if (filter.enabled()) {
            Response response = filter.afterReceiveResponse(invocation, responseEx);
            if (response != null) {
              complete(response);
              return;
            }
          }
        }
      } catch (Throwable e) {
        // already collection time from httpSocketMetric
        // and connection maybe already belongs to other invocation in this time
        // so set connection to null
        fail(null, e);
      }
    });
  }

  protected void complete(Response response) {
    invocation.getInvocationStageTrace().finishClientFiltersResponse();
    asyncResp.complete(response);
  }

  protected void fail(ConnectionBase connection, Throwable e) {
    if (alreadyFailed) {
      return;
    }

    alreadyFailed = true;

    InvocationStageTrace stageTrace = invocation.getInvocationStageTrace();
    // connection maybe null when exception happens such as ssl handshake failure
    if (connection != null) {
      DefaultHttpSocketMetric httpSocketMetric = (DefaultHttpSocketMetric) connection.metric();
      stageTrace.finishGetConnection(httpSocketMetric.getRequestBeginTime());
      stageTrace.finishWriteToBuffer(httpSocketMetric.getRequestEndTime());
    }

    // even failed and did not received response, still set time for it
    // that will help to know the real timeout time
    if (stageTrace.getFinishReceiveResponse() == 0) {
      stageTrace.finishReceiveResponse();
    }
    if (stageTrace.getStartClientFiltersResponse() == 0) {
      stageTrace.startClientFiltersResponse();
    }

    stageTrace.finishClientFiltersResponse();

    try {
      if (e instanceof TimeoutException) {
        // give an accurate cause for timeout exception
        //   The timeout period of 30000ms has been exceeded while executing GET /xxx for server 1.1.1.1:8080
        // should not copy the message to invocationException to avoid leak server ip address
        LOGGER.info("Request timeout, Details: {}.", e.getMessage());
        asyncResp.consumerFail(new InvocationException(Status.REQUEST_TIMEOUT,
            new CommonExceptionData("Request Timeout.")));
        return;
      }
      asyncResp.fail(invocation.getInvocationType(), e);
    } catch (Throwable e1) {
      invocation.getTraceIdLogger().error(LOGGER, "failed to invoke asyncResp, message={}"
          , ExceptionUtils.getExceptionMessageWithoutTrace(e));
    }
  }

  protected void setCseContext() {
    try {
      clientRequest.putHeader(Const.CSE_CONTEXT, JsonUtils.writeValueAsString(invocation.getContext()));
    } catch (Throwable e) {
      invocation.getTraceIdLogger().error(LOGGER, "Failed to encode and set cseContext, message={}."
          , ExceptionUtils.getExceptionMessageWithoutTrace(e));
    }
  }

  protected String createRequestPath(RestOperationMeta swaggerRestOperation) throws Exception {
    URIEndpointObject address = (URIEndpointObject) invocation.getEndpoint().getAddress();
    String urlPrefix = address.getFirst(DefinitionConst.URL_PREFIX);

    String path = (String) invocation.getHandlerContext().get(RestConst.REST_CLIENT_REQUEST_PATH);
    if (path == null) {
      path = swaggerRestOperation.getPathBuilder().createRequestPath(invocation.getSwaggerArguments());
    }

    if (StringUtils.isEmpty(urlPrefix) || path.startsWith(urlPrefix)) {
      return path;
    }

    return urlPrefix + path;
  }
}
