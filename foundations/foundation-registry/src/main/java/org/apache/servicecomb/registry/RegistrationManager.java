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

package org.apache.servicecomb.registry;

import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.utils.URIBuilder;
import org.apache.servicecomb.foundation.common.event.EnableExceptionPropagation;
import org.apache.servicecomb.foundation.common.event.EventManager;
import org.apache.servicecomb.foundation.common.net.IpPort;
import org.apache.servicecomb.foundation.common.net.NetUtils;
import org.apache.servicecomb.foundation.common.utils.BeanUtils;
import org.apache.servicecomb.foundation.common.utils.SPIEnabled;
import org.apache.servicecomb.foundation.common.utils.SPIServiceUtils;
import org.apache.servicecomb.registry.api.Registration;
import org.apache.servicecomb.registry.api.event.MicroserviceInstanceRegisteredEvent;
import org.apache.servicecomb.registry.api.registry.BasePath;
import org.apache.servicecomb.registry.api.registry.Microservice;
import org.apache.servicecomb.registry.api.registry.MicroserviceInstance;
import org.apache.servicecomb.registry.api.registry.MicroserviceInstanceStatus;
import org.apache.servicecomb.registry.consumer.MicroserviceManager;
import org.apache.servicecomb.registry.consumer.StaticMicroserviceVersions;
import org.apache.servicecomb.registry.definition.MicroserviceNameParser;
import org.apache.servicecomb.registry.swagger.SwaggerLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.netflix.config.DynamicPropertyFactory;

import io.vertx.core.json.jackson.JacksonFactory;
import io.vertx.core.spi.json.JsonCodec;

public class RegistrationManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationManager.class);

  // value is ip or {interface name}
  public static final String PUBLISH_ADDRESS = "servicecomb.service.publishAddress";

  private static final String PUBLISH_PORT = "servicecomb.{transport_name}.publishPort";

  private static SwaggerLoader swaggerLoader = new SwaggerLoader();

  public static RegistrationManager INSTANCE = new RegistrationManager();

  private final List<Registration> registrationList = new ArrayList<>();

  private Registration primary;

  private RegistrationManager() {
    SPIServiceUtils.getOrLoadSortedService(Registration.class)
        .stream()
        .filter((SPIEnabled::enabled))
        .forEach(registrationList::add);
    initPrimary();
  }

  public MicroserviceInstance getMicroserviceInstance() {
    return primary.getMicroserviceInstance();
  }

  public Microservice getMicroservice() {
    assertPrimaryNotNull();
    return primary.getMicroservice();
  }

  private void assertPrimaryNotNull() {
    if (primary == null) {
      throw new NullPointerException("At least one Registration implementation configured. Missed"
          + " to include dependency ? e.g. <artifactId>registry-service-center</artifactId>");
    }
  }

  public String getAppId() {
    assertPrimaryNotNull();
    return primary.getAppId();
  }

  public SwaggerLoader getSwaggerLoader() {
    return swaggerLoader;
  }

  public void updateMicroserviceInstanceStatus(
      MicroserviceInstanceStatus status) {
    registrationList
        .forEach(registration -> registration.updateMicroserviceInstanceStatus(status));
  }

  public void addSchema(String schemaId, String content) {
    registrationList
        .forEach(registration -> registration.addSchema(schemaId, content));
  }

  public void addEndpoint(String endpoint) {
    registrationList
        .forEach(registration -> registration.addEndpoint(endpoint));
  }

  public void addBasePath(Collection<BasePath> basePaths) {
    registrationList
        .forEach(registration -> registration.addBasePath(basePaths));
  }

  public void destroy() {
    registrationList.forEach(registration -> registration.destroy());
  }

  public void run() {
    EventManager.getEventBus().register(new AfterServiceInstanceRegistryHandler(registrationList.size()));
    registrationList.forEach(registration -> registration.run());
  }

  public void init() {
    BeanUtils.addBeans(Registration.class, registrationList);

    initPrimary();
    registrationList.forEach(registration -> registration.init());
  }

  private void initPrimary() {
    if (registrationList.isEmpty()) {
      LOGGER.warn("No registration is enabled. Fix this if only in unit tests.");
      primary = null;
    } else {
      primary = registrationList.get(0);
    }
  }

  /**
   * <p>
   * Register a third party service if not registered before, and set it's instances into
   * {@linkplain StaticMicroserviceVersions StaticMicroserviceVersions}.
   * </p>
   * <p>
   * The registered third party service has the same {@code appId} and {@code environment} as this microservice instance has,
   * and there is only one schema represented by {@code schemaIntfCls}, whose name is the same as {@code microserviceName}.
   * </p>
   * <em>
   *   This method is for initializing 3rd party service endpoint config.
   *   i.e. If this service has not been registered before, this service will be registered and the instances will be set;
   *   otherwise, NOTHING will happen.
   * </em>
   *
   * @param microserviceName name of the 3rd party service, and this param also specifies the schemaId
   * @param version version of this 3rd party service
   * @param instances the instances of this 3rd party service. Users only need to specify the endpoint information, other
   * necessary information will be generate and set in the implementation of this method.
   * @param schemaIntfCls the producer interface of the service. This interface is used to generate swagger schema and
   * can also be used for the proxy interface of RPC style invocation.
   */
  public void registerMicroserviceMapping(String microserviceName, String version, List<MicroserviceInstance> instances,
      Class<?> schemaIntfCls) {
    MicroserviceNameParser parser = new MicroserviceNameParser(getAppId(), microserviceName);
    MicroserviceManager microserviceManager = DiscoveryManager.INSTANCE.getAppManager()
        .getOrCreateMicroserviceManager(parser.getAppId());
    microserviceManager.getVersionsByName()
        .computeIfAbsent(microserviceName,
            svcName -> new StaticMicroserviceVersions(DiscoveryManager.INSTANCE.getAppManager(), parser.getAppId(),
                microserviceName)
                .init(schemaIntfCls, version, instances)
        );
  }

  /**
   * @see #registerMicroserviceMapping(String, String, List, Class)
   * @param endpoints the endpoints of 3rd party service. Each of endpoints will be treated as a separated instance.
   * Format of the endpoints is the same as the endpoints that ServiceComb microservices register in service-center,
   * like {@code rest://127.0.0.1:8080}
   */
  public void registerMicroserviceMappingByEndpoints(String microserviceName, String version,
      List<String> endpoints, Class<?> schemaIntfCls) {
    ArrayList<MicroserviceInstance> microserviceInstances = new ArrayList<>();
    for (String endpoint : endpoints) {
      MicroserviceInstance instance = new MicroserviceInstance();
      instance.setEndpoints(Collections.singletonList(endpoint));
      microserviceInstances.add(instance);
    }

    registerMicroserviceMapping(microserviceName, version, microserviceInstances, schemaIntfCls);
  }

  public static String getPublishAddress() {
    String publicAddressSetting =
        DynamicPropertyFactory.getInstance().getStringProperty(PUBLISH_ADDRESS, "").get();
    publicAddressSetting = publicAddressSetting.trim();
    if (publicAddressSetting.isEmpty()) {
      return NetUtils.getHostAddress();
    }

    // placeholder is network interface name
    if (publicAddressSetting.startsWith("{") && publicAddressSetting.endsWith("}")) {
      return NetUtils
          .ensureGetInterfaceAddress(publicAddressSetting.substring(1, publicAddressSetting.length() - 1))
          .getHostAddress();
    }

    return publicAddressSetting;
  }

  public static String getPublishHostName() {
    String publicAddressSetting =
        DynamicPropertyFactory.getInstance().getStringProperty(PUBLISH_ADDRESS, "").get();
    publicAddressSetting = publicAddressSetting.trim();
    if (publicAddressSetting.isEmpty()) {
      return NetUtils.getHostName();
    }

    if (publicAddressSetting.startsWith("{") && publicAddressSetting.endsWith("}")) {
      return NetUtils
          .ensureGetInterfaceAddress(publicAddressSetting.substring(1, publicAddressSetting.length() - 1))
          .getHostName();
    }

    return publicAddressSetting;
  }

  /**
   * In the case that listening address configured as 0.0.0.0, the publish address will be determined
   * by the query result for the net interfaces.
   *
   * @return the publish address, or {@code null} if the param {@code address} is null.
   */
  public static String getPublishAddress(String schema, String address) {
    if (address == null) {
      return address;
    }

    try {
      URI originalURI = new URI(schema + "://" + address);
      IpPort ipPort = NetUtils.parseIpPort(originalURI);
      if (ipPort == null) {
        LOGGER.warn("address {} not valid.", address);
        return null;
      }

      IpPort publishIpPort = genPublishIpPort(schema, ipPort);
      URIBuilder builder = new URIBuilder(originalURI);
      return builder.setHost(publishIpPort.getHostOrIp()).setPort(publishIpPort.getPort()).build().toString();
    } catch (URISyntaxException e) {
      LOGGER.warn("address {} not valid.", address);
      return null;
    }
  }

  private static IpPort genPublishIpPort(String schema, IpPort ipPort) {
    String publicAddressSetting = DynamicPropertyFactory.getInstance()
        .getStringProperty(PUBLISH_ADDRESS, "")
        .get();
    publicAddressSetting = publicAddressSetting.trim();

    if (publicAddressSetting.isEmpty()) {
      InetSocketAddress socketAddress = ipPort.getSocketAddress();
      if (socketAddress.getAddress().isAnyLocalAddress()) {
        String host = NetUtils.getHostAddress();
        if (Inet6Address.class.isInstance(socketAddress.getAddress())) {
          host = NetUtils.getIpv6HostAddress();
        }
        LOGGER.warn("address {}, auto select a host address to publish {}:{}, maybe not the correct one",
            socketAddress,
            host,
            socketAddress.getPort());
        return new IpPort(host, ipPort.getPort());
      }

      return ipPort;
    }

    if (publicAddressSetting.startsWith("{") && publicAddressSetting.endsWith("}")) {
      publicAddressSetting = NetUtils
          .ensureGetInterfaceAddress(
              publicAddressSetting.substring(1, publicAddressSetting.length() - 1))
          .getHostAddress();
    }

    String publishPortKey = PUBLISH_PORT.replace("{transport_name}", schema);
    int publishPortSetting = DynamicPropertyFactory.getInstance()
        .getIntProperty(publishPortKey, 0)
        .get();
    int publishPort = publishPortSetting == 0 ? ipPort.getPort() : publishPortSetting;
    return new IpPort(publicAddressSetting, publishPort);
  }

  public String info() {
    StringBuilder result = new StringBuilder();
    AtomicBoolean first = new AtomicBoolean(true);
    registrationList.forEach(registration -> {
      if (first.getAndSet(false)) {
        result.append("App ID: " + registration.getAppId() + "\n");
        result.append("Service Name: " + registration.getMicroservice().getServiceName() + "\n");
        result.append("Version: " + registration.getMicroservice().getVersion() + "\n");
        result.append("Environment: " + registration.getMicroservice().getEnvironment() + "\n");
        result.append("Endpoints: " + getEndpoints(registration.getMicroserviceInstance().getEndpoints()) + "\n");
        result.append("Registration implementations:\n");
      }

      result.append("  name:" + registration.name() + "\n");
      result.append("    Service ID: " + registration.getMicroservice().getServiceId() + "\n");
      result.append("    Instance ID: " + registration.getMicroserviceInstance().getInstanceId() + "\n");
    });
    return result.toString();
  }

  private String getEndpoints(List<String> endpoints) {
    return JacksonFactory.CODEC.toString(endpoints);
  }

  public static class AfterServiceInstanceRegistryHandler {
    private AtomicInteger instanceRegisterCounter;

    AfterServiceInstanceRegistryHandler(int counter) {
      instanceRegisterCounter = new AtomicInteger(counter);
    }

    @Subscribe
    @EnableExceptionPropagation
    public void afterRegistryInstance(MicroserviceInstanceRegisteredEvent event) {
      LOGGER.info("receive MicroserviceInstanceRegisteredEvent event, registration={}, instance id={}",
          event.getRegistrationName(),
          event.getInstanceId());

      if (instanceRegisterCounter.decrementAndGet() > 0) {
        return;
      }

      EventManager.unregister(this);

      EventManager.getEventBus().post(new MicroserviceInstanceRegisteredEvent(
          "Registration Manager",
          null,
          true
      ));
    }
  }
}