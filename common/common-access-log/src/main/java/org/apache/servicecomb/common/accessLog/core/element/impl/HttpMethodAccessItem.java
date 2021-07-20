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

package org.apache.servicecomb.common.accessLog.core.element.impl;


import org.apache.commons.lang3.StringUtils;
import org.apache.servicecomb.common.accessLog.core.element.AccessLogItem;
import org.apache.servicecomb.common.rest.RestConst;
import org.apache.servicecomb.common.rest.codec.param.RestClientRequestImpl;
import org.apache.servicecomb.core.definition.OperationMeta;
import org.apache.servicecomb.core.event.InvocationFinishEvent;
import org.apache.servicecomb.core.event.ServerAccessLogEvent;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * HTTP method
 */
public class HttpMethodAccessItem implements AccessLogItem<RoutingContext> {

  public static final String EMPTY_RESULT = "-";

  @Override
  public void appendServerFormattedItem(ServerAccessLogEvent accessLogEvent, StringBuilder builder) {
    HttpServerRequest request = accessLogEvent.getRoutingContext().request();
    if (null == request || null == request.method()) {
      builder.append(EMPTY_RESULT);
      return;
    }
    builder.append(request.method().toString());
  }

  @Override
  public void appendClientFormattedItem(InvocationFinishEvent finishEvent, StringBuilder builder) {
    OperationMeta operationMeta = finishEvent.getInvocation().getOperationMeta();
    if (operationMeta != null && !StringUtils.isEmpty(operationMeta.getHttpMethod())) {
      builder.append(operationMeta.getHttpMethod());
      return;
    }
    RestClientRequestImpl restRequestImpl = (RestClientRequestImpl) finishEvent.getInvocation().getHandlerContext()
        .get(RestConst.INVOCATION_HANDLER_REQUESTCLIENT);
    if (null == restRequestImpl || null == restRequestImpl.getRequest()
        || null == restRequestImpl.getRequest().getMethod()) {
      builder.append(EMPTY_RESULT);
      return;
    }
    builder.append(restRequestImpl.getRequest().getMethod().toString());
  }
}
