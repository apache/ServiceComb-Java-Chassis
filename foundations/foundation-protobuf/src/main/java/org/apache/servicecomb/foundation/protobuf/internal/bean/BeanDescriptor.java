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
package org.apache.servicecomb.foundation.protobuf.internal.bean;

import java.util.HashMap;
import java.util.Map;

import org.apache.servicecomb.foundation.common.utils.LambdaMetafactoryUtils;
import org.apache.servicecomb.foundation.common.utils.bean.Getter;
import org.apache.servicecomb.foundation.common.utils.bean.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

public class BeanDescriptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(BeanDescriptor.class);

  private JavaType javaType;

  private Map<String, Getter> getters = new HashMap<>();

  private Map<String, Setter> setters = new HashMap<>();

  public JavaType getJavaType() {
    return javaType;
  }

  public Map<String, Getter> getGetters() {
    return getters;
  }

  public Map<String, Setter> getSetters() {
    return setters;
  }

  public void init(SerializationConfig serializationConfig, JavaType javaType) {
    this.javaType = javaType;

    BeanDescription beanDescription = serializationConfig.introspect(javaType);
    for (BeanPropertyDefinition propertyDefinition : beanDescription.findProperties()) {
      try {
        initGetter(propertyDefinition);
      } catch (Throwable e) {
        LOGGER.error("failed to init getter for field {}:{}", javaType.getRawClass().getName(),
            propertyDefinition.getName(), e);
      }

      try {
        initSetter(propertyDefinition);
      } catch (Throwable e) {
        LOGGER.error("failed to init setter for field {}:{}", javaType.getRawClass().getName(),
            propertyDefinition.getName(), e);
      }
    }
  }

  protected void initGetter(BeanPropertyDefinition propertyDefinition) throws Throwable {
    if (propertyDefinition.hasGetter()) {
      getters.put(propertyDefinition.getName(),
          LambdaMetafactoryUtils.createGetter(propertyDefinition.getGetter().getAnnotated()));
      return;
    }

    if (propertyDefinition.hasField() && propertyDefinition.getField().isPublic()) {
      getters.put(propertyDefinition.getName(),
          LambdaMetafactoryUtils.createGetter(propertyDefinition.getField().getAnnotated()));
    }
  }

  protected void initSetter(BeanPropertyDefinition propertyDefinition) throws Throwable {
    if (propertyDefinition.hasSetter()) {
      setters.put(propertyDefinition.getName(),
          LambdaMetafactoryUtils.createSetter(propertyDefinition.getSetter().getAnnotated()));
      return;
    }

    if (propertyDefinition.hasField() && propertyDefinition.getField().isPublic()) {
      setters.put(propertyDefinition.getName(),
          LambdaMetafactoryUtils.createSetter(propertyDefinition.getField().getAnnotated()));
    }
  }
}
