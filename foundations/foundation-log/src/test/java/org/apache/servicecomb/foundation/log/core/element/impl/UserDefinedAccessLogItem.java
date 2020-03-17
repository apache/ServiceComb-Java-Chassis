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

package org.apache.servicecomb.foundation.log.core.element.impl;


import org.apache.servicecomb.core.event.InvocationFinishEvent;
import org.apache.servicecomb.core.event.ServerAccessLogEvent;
import org.apache.servicecomb.foundation.log.core.element.LogItem;

import io.vertx.ext.web.RoutingContext;

/**
 * For access log extension test
 */
public class UserDefinedAccessLogItem implements LogItem<RoutingContext> {
  private String config;

  public UserDefinedAccessLogItem(String config) {
    this.config = config;
  }

  @Override
  public void appendFormattedItem(ServerAccessLogEvent accessLogEvent, StringBuilder builder) {
    builder.append("user-defined-")
        .append(config);
  }

  @Override
  public void appendFormattedItem(InvocationFinishEvent clientLogEvent, StringBuilder builder) {
    builder.append("user-defined-")
        .append(config);
  }
}