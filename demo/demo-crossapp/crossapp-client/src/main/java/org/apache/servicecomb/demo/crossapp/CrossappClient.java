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

package org.apache.servicecomb.demo.crossapp;

import java.util.Collections;
import java.util.TreeSet;

import org.apache.servicecomb.core.provider.consumer.InvokerUtils;
import org.apache.servicecomb.demo.TestMgr;
import org.apache.servicecomb.foundation.common.utils.BeanUtils;
import org.apache.servicecomb.foundation.common.utils.Log4jUtils;
import org.apache.servicecomb.provider.pojo.RpcReference;
import org.apache.servicecomb.provider.springmvc.reference.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class CrossappClient {
  @RpcReference(microserviceName = "appServer:appService", schemaId = "helloworld")
  private static HelloWorld helloWorld;

  public static void main(String[] args) throws Exception {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    Log4jUtils.init();
    BeanUtils.init();

    run();

    TestMgr.summary();
    System.setProperty("sun.net.http.allowRestrictedHeaders", "false");
  }

  @SuppressWarnings({"deprecation"})
  public static void run() {
    Object result = InvokerUtils.syncInvoke("appServer:appService", "helloworld", "sayHello", null);
    TestMgr.check("hello world", result);

    RestTemplate restTemplate = RestTemplateBuilder.create();
    result = restTemplate.getForObject("cse://appServer:appService/helloworld/hello", String.class);
    TestMgr.check("hello world", result);
    result = restTemplate.getForObject("servicecomb://appServer:appService/helloworld/hello", String.class);
    TestMgr.check("hello world", result);

    result = helloWorld.sayHello();
    TestMgr.check("hello world", result);

    testCorsHandler();
  }

  private static void testCorsHandler() {
    RestTemplate springRestTemplate = new RestTemplate();
    MultiValueMap<String, String> requestHeaders = new LinkedMultiValueMap<>();
    requestHeaders.put("Origin", Collections.singletonList("http://localhost:8080"));
    requestHeaders.put("Access-Control-Request-Method", Collections.singletonList("PUT"));

    HttpEntity<Object> requestEntity = new HttpEntity<>(requestHeaders);
    ResponseEntity<String> responseEntity = springRestTemplate
        .exchange("http://127.0.0.1:8080/helloworld/hello", HttpMethod.OPTIONS, requestEntity,
            String.class);

    TestMgr.check("204", responseEntity.getStatusCodeValue());
    TreeSet<String> sortedSet = new TreeSet<>(responseEntity.getHeaders().get("Access-Control-Allow-Methods"));
    TestMgr.check("[DELETE,POST,GET,PUT]", sortedSet);
    sortedSet = new TreeSet<>(responseEntity.getHeaders().get("Access-Control-Allow-Headers"));
    TestMgr.check("[abc,def]", sortedSet);
    TestMgr.check("*", responseEntity.getHeaders().getFirst("Access-Control-Allow-Origin"));
  }
}
