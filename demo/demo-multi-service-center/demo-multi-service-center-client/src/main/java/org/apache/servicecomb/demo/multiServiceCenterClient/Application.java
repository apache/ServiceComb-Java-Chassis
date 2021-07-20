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

package org.apache.servicecomb.demo.multiServiceCenterClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.servicecomb.demo.CategorizedTestCaseRunner;
import org.apache.servicecomb.demo.TestMgr;
import org.apache.servicecomb.springboot2.starter.EnableServiceComb;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.stereotype.Component;

@SpringBootApplication
@EnableServiceComb
@Component
public class Application {

  public static void main(final String[] args) throws Exception {
    new SpringApplicationBuilder().sources(Application.class)
        .web(WebApplicationType.NONE).build().run(args);

    runTest();
  }

  public static void runTest() throws Exception {
    CategorizedTestCaseRunner.runCategorizedTestCase("demo-multi-service-center-serverA");
    testRegistryThreads();
    TestMgr.summary();
    if (!TestMgr.errors().isEmpty()) {
      throw new IllegalStateException("tests failed");
    }
  }

  private static void testRegistryThreads() throws Exception {
    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
    List<String> expectedThread = new ArrayList<>();
    threadSet.forEach(thread -> {
      if (thread.getName().contains("registry-")) {
        expectedThread.add(thread.getName());
      }
    });
    //registry-watch-vert.x-eventloop-thread-3(option)
    //registry-watch-vert.x-eventloop-thread-2(option)
    //registry-watch-vert.x-eventloop-thread-1
    //registry-watch-vert.x-eventloop-thread-0
    //registry-watch-serverB-vert.x-eventloop-thread-3(option)
    //registry-watch-serverB-vert.x-eventloop-thread-2(option)
    //registry-watch-serverB-vert.x-eventloop-thread-1
    //registry-watch-serverB-vert.x-eventloop-thread-0
    //registry-watch-pool-worker-service-center-watch-1
    //registry-watch-pool-worker-service-center-watch-0
    //registry-watch-serverB-pool-worker-service-center-watch-1
    //registry-watch-serverB-pool-worker-service-center-watch-0
    //registry-vert.x-eventloop-thread-3(option)
    //registry-vert.x-eventloop-thread-2(option)
    //registry-vert.x-eventloop-thread-1
    //registry-vert.x-eventloop-thread-0
    //registry-serverB-vert.x-eventloop-thread-3(option)
    //registry-serverB-vert.x-eventloop-thread-2(option)
    //registry-serverB-vert.x-eventloop-thread-1
    //registry-serverB-vert.x-eventloop-thread-0
    TestMgr.check(expectedThread.size() >= 12, expectedThread.size() <= 20);
  }
}
