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
package org.apache.servicecomb.foundation.protobuf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RequestRootDeserializer<T> {
  private boolean wrapArgument;

  private String parameterName;

  private RootDeserializer<T> rootDeserializer;

  public RequestRootDeserializer(RootDeserializer<T> rootDeserializer) {
    this(rootDeserializer, true, null);
  }

  public RequestRootDeserializer(RootDeserializer<T> rootDeserializer, boolean wrapArgument, String parameterName) {
    this.rootDeserializer = rootDeserializer;
    this.wrapArgument = wrapArgument;
    this.parameterName = parameterName;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> deserialize(byte[] bytes) throws IOException {
    if (!wrapArgument) {
      if (parameterName == null) {
        return null;
      }
      Map<String, Object> result = new HashMap<>(1);
      result.put(parameterName, rootDeserializer.deserialize(bytes));
      return result;
    } else {
      return (Map<String, Object>) rootDeserializer.deserialize(bytes);
    }
  }
}
