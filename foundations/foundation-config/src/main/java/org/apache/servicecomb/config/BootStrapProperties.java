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

package org.apache.servicecomb.config;

import java.util.*;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertyConverter;
import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

/**
 * This class holds configurations that need to be configured
 * through property files or environment variables.
 */
public class BootStrapProperties {
  // start of : service definition keys
  // service definition keys of old version
  public static final String OLD_CONFIG_SERVICE_APPLICATION = "APPLICATION_ID";

  public static final String OLD_CONFIG_SERVICE_NAME = "service_description.name";

  public static final String OLD_CONFIG_SERVICE_VERSION = "service_description.version";

  public static final String OLD_CONFIG_SERVICE_ROLE = "service_description.role";

  public static final String OLD_CONFIG_SERVICE_DESCRIPTION = "service_description.description";

  public static final String OLD_CONFIG_SERVICE_ENVIRONMENT = "service_description.environment";

  public static final String OLD_CONFIG_SERVICE_EXTENDED_CLASS = "service_description.propertyExtendedClass";

  public static final String OLD_CONFIG_SERVICE_PROPERTIES = "service_description.properties";

  public static final String OLD_CONFIG_SERVICE_PATHS = "service_description.paths";

  // service instance definition keys of old version
  public static final String OLD_CONFIG_SERVICE_INSTANCE_PROPERTIES = "instance_description.properties";

  public static final String OLD_CONFIG_SERVICE_INSTANCE_EXTENDED_CLASS = "instance_description.propertyExtendedClass";

  public static final String OLD_CONFIG_SERVICE_INSTANCE_ENVIRONMENT = "instance_description.environment";

  public static final String OLD_CONFIG_SERVICE_INSTANCE_INITIAL_STATUS = "instance_description.initialStatus";

  private static final String OLD_CONFIG_SERVICE_INSTANCE_TAGS = "instance_description.properties.tags";

  // service definition keys of new version
  public static final String CONFIG_SERVICE_APPLICATION = "servicecomb.service.application";

  public static final String CONFIG_SERVICE_NAME = "servicecomb.service.name";

  public static final String CONFIG_SERVICE_VERSION = "servicecomb.service.version";

  public static final String CONFIG_SERVICE_VERSION_AUTO_GENERATE = "servicecomb.service.versionAutoGenerate";

  public static final String CONFIG_SERVICE_ROLE = "servicecomb.service.role";

  public static final String CONFIG_SERVICE_DESCRIPTION = "servicecomb.service.description";

  public static final String CONFIG_SERVICE_ENVIRONMENT = "servicecomb.service.environment";

  public static final String CONFIG_SERVICE_EXTENDED_CLASS = "servicecomb.service.propertyExtendedClass";

  public static final String CONFIG_SERVICE_PROPERTIES = "servicecomb.service.properties";

  public static final String CONFIG_SERVICE_PATHS = "servicecomb.service.paths";

  // service instance definition keys of new version
  public static final String CONFIG_SERVICE_INSTANCE_PROPERTIES = "servicecomb.instance.properties";

  public static final String CONFIG_SERVICE_INSTANCE_EXTENDED_CLASS = "servicecomb.instance.propertyExtendedClass";

  public static final String CONFIG_SERVICE_INSTANCE_ENVIRONMENT = "servicecomb.instance.environment";

  public static final String CONFIG_SERVICE_INSTANCE_INITIAL_STATUS = "servicecomb.instance.initialStatus";

  private static final String CONFIG_SERVICE_INSTANCE_TAGS = "servicecomb.instance.properties.tags";

  // configuration default values
  public static final String DEFAULT_APPLICATION = "default";

  public static final String DEFAULT_MICROSERVICE_NAME = "defaultMicroservice";

  public static final String DEFAULT_MICROSERVICE_VERSION = "1.0.0.0";

  public static final String DEFAULT_MICROSERVICE_ROLE = "FRONT";

  public static final String DEFAULT_MICROSERVICE_ENVIRONMENT = "";

  public static final String DEFAULT_MICROSERVICE_INSTANCE_ENVIRONMENT = "production";

  public static final String DEFAULT_MICROSERVICE_INSTANCE_INITIAL_STATUS = "UP";

  private static final Configuration configuration = ConfigUtil.createLocalConfig();

  public static String readApplication(Configuration configuration) {
    return readStringValue(configuration, CONFIG_SERVICE_APPLICATION,
        OLD_CONFIG_SERVICE_APPLICATION, DEFAULT_APPLICATION);
  }

  public static String readApplication() {
    return readApplication(BootStrapProperties.configuration);
  }

  public static String readServiceName(Configuration configuration) {
    String result = readStringValue(configuration, CONFIG_SERVICE_NAME, OLD_CONFIG_SERVICE_NAME,
        DEFAULT_MICROSERVICE_NAME);
    checkMicroserviceName(result);
    return result;
  }

  public static String readServiceName() {
    return readServiceName(BootStrapProperties.configuration);
  }

  public static String readServiceVersion(Configuration configuration) {
    String version = readStringValue(configuration, CONFIG_SERVICE_VERSION, OLD_CONFIG_SERVICE_VERSION,
            "");
    String serviceVersionAuto = readServiceVersionAuto(configuration);
    if(StringUtils.isBlank(version)){
      if(StringUtils.equalsAnyIgnoreCase(serviceVersionAuto, "timestamp")){
        version = DateFormatUtils.format(new Date(), "yyyy.MMdd.HHmm.ss");
      }
      if(StringUtils.isBlank(version)){
        version = DEFAULT_MICROSERVICE_VERSION;
      }
    }
    return version;
  }

  /**
   * 自动生成版本号（默认为空，不使用）
   * @param configuration
   * @return timestamp:使用时间格式生成
   */
  public static String readServiceVersionAuto(Configuration configuration) {
    return readStringValue(configuration, CONFIG_SERVICE_VERSION_AUTO_GENERATE, "",
            "");
  }

  public static String readServiceVersion() {
    return readServiceVersion(BootStrapProperties.configuration);
  }

  public static String readServiceRole(Configuration configuration) {
    return readStringValue(configuration, CONFIG_SERVICE_ROLE, OLD_CONFIG_SERVICE_ROLE, DEFAULT_MICROSERVICE_ROLE);
  }

  public static String readServiceRole() {
    return readServiceRole(BootStrapProperties.configuration);
  }

  public static String readServiceDescription(Configuration configuration) {
    String[] descriptionArray = configuration.getStringArray(CONFIG_SERVICE_DESCRIPTION);
    if (null == descriptionArray || descriptionArray.length < 1) {
      descriptionArray = configuration.getStringArray(OLD_CONFIG_SERVICE_DESCRIPTION);
    }

    if (null == descriptionArray || descriptionArray.length < 1) {
      return null;
    }

    StringBuilder rawDescriptionBuilder = new StringBuilder();
    for (String desc : descriptionArray) {
      rawDescriptionBuilder.append(desc).append(",");
    }

    return rawDescriptionBuilder.substring(0, rawDescriptionBuilder.length() - 1);
  }

  public static String readServiceDescription() {
    return readServiceDescription(BootStrapProperties.configuration);
  }

  public static String readServiceEnvironment(Configuration configuration) {
    return readStringValue(configuration, CONFIG_SERVICE_ENVIRONMENT, OLD_CONFIG_SERVICE_ENVIRONMENT,
        DEFAULT_MICROSERVICE_ENVIRONMENT);
  }

  public static String readServiceEnvironment() {
    return readServiceEnvironment(BootStrapProperties.configuration);
  }

  public static String readServiceExtendedClass(Configuration configuration) {
    return readStringValue(configuration, CONFIG_SERVICE_EXTENDED_CLASS, OLD_CONFIG_SERVICE_EXTENDED_CLASS,
        null);
  }

  public static String readServiceExtendedClass() {
    return readServiceExtendedClass(BootStrapProperties.configuration);
  }

  public static Map<String, String> readServiceProperties(Configuration configuration) {
    return readProperties(configuration, CONFIG_SERVICE_PROPERTIES, OLD_CONFIG_SERVICE_PROPERTIES);
  }

  public static Map<String, String> readServiceProperties() {
    return readServiceProperties(BootStrapProperties.configuration);
  }

  public static List<Object> readServicePaths(Configuration configuration) {
    List<Object> result = configuration.getList(CONFIG_SERVICE_PATHS);
    if (result == null || result.isEmpty()) {
      result = configuration.getList(OLD_CONFIG_SERVICE_PATHS);
    }
    return result;
  }

  public static List<Object> readServicePaths() {
    return readServicePaths(BootStrapProperties.configuration);
  }

  public static Map<String, String> readServiceInstanceProperties(Configuration configuration) {
    return readProperties(configuration, CONFIG_SERVICE_INSTANCE_PROPERTIES, OLD_CONFIG_SERVICE_INSTANCE_PROPERTIES);
  }

  public static Map<String, String> readServiceInstanceProperties() {
    return readServiceInstanceProperties(BootStrapProperties.configuration);
  }

  public static String readServiceInstanceExtendedClass(Configuration configuration) {
    return readStringValue(configuration, CONFIG_SERVICE_INSTANCE_EXTENDED_CLASS,
        OLD_CONFIG_SERVICE_INSTANCE_EXTENDED_CLASS,
        null);
  }

  public static String readServiceInstanceExtendedClass() {
    return readServiceInstanceExtendedClass(BootStrapProperties.configuration);
  }

  public static String readServiceInstanceEnvironment(Configuration configuration) {
    return readStringValue(configuration, CONFIG_SERVICE_INSTANCE_ENVIRONMENT,
        OLD_CONFIG_SERVICE_INSTANCE_ENVIRONMENT,
        DEFAULT_MICROSERVICE_INSTANCE_ENVIRONMENT);
  }

  public static String readServiceInstanceEnvironment() {
    return readServiceInstanceEnvironment(BootStrapProperties.configuration);
  }

  public static String readServiceInstanceInitialStatus(Configuration configuration) {
    return readStringValue(configuration, CONFIG_SERVICE_INSTANCE_INITIAL_STATUS,
        OLD_CONFIG_SERVICE_INSTANCE_INITIAL_STATUS,
        DEFAULT_MICROSERVICE_INSTANCE_INITIAL_STATUS);
  }

  public static String readServiceInstanceInitialStatus() {
    return readServiceInstanceInitialStatus(BootStrapProperties.configuration);
  }

  public static String readServiceInstanceTags(Configuration configuration) {
    return readStringValue(configuration, CONFIG_SERVICE_INSTANCE_TAGS,
        OLD_CONFIG_SERVICE_INSTANCE_TAGS,
        null);
  }

  public static String readServiceInstanceTags() {
    return readServiceInstanceTags(BootStrapProperties.configuration);
  }

  private static String readStringValue(Configuration configuration, String newKey, String oldKey,
      String defaultValue) {
    String result = configuration.getString(newKey, null);
    if (result == null) {
      return configuration.getString(oldKey, defaultValue);
    }
    return result;
  }

  private static Map<String, String> readProperties(Configuration configuration, String newKey, String oldKey) {
    AbstractConfiguration subset = (AbstractConfiguration) configuration.subset(newKey);
    if (subset.isEmpty()) {
      subset = (AbstractConfiguration) configuration.subset(oldKey);
    }
    return toStringMap(subset);
  }

  private static Map<String, String> toStringMap(AbstractConfiguration configuration) {
    AbstractConfiguration root = findRoot(configuration);
    Map<String, String> map = new LinkedHashMap<>();
    configuration.getKeys().forEachRemaining(key -> {
      Object value = configuration.getProperty(key);
      // support placeholder
      value = PropertyConverter.interpolate(value, root);
      map.put(key, Objects.toString(value, null));
    });
    return map;
  }

  private static AbstractConfiguration findRoot(AbstractConfiguration configuration) {
    if (configuration instanceof SubsetConfiguration) {
      return findRoot((AbstractConfiguration) ((SubsetConfiguration) configuration).getParent());
    }

    return configuration;
  }

  private static void checkMicroserviceName(String name) {
    // the configuration we used
    // when resolve placeholder failed
    // the result will remains ${var}
    if (StringUtils.isEmpty(name) || name.contains("${")) {
      throw new IllegalArgumentException(String.format(
          "MicroserviceName '%s' is invalid. you must configure '%s' or set the placeholder value.",
          name,
          CONFIG_SERVICE_NAME));
    }
  }
}
