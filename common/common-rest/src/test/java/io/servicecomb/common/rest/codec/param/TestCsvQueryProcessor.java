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
package io.servicecomb.common.rest.codec.param;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.type.TypeFactory;

import io.servicecomb.common.rest.codec.param.QueryProcessorCreator.CsvQueryProcessor;
import mockit.Expectations;
import mockit.Mocked;

public class TestCsvQueryProcessor {
  @Mocked
  HttpServletRequest request;

  private ParamValueProcessor createProcessor(String name, Class<?> type) {
    return new CsvQueryProcessor(name, TypeFactory.defaultInstance().constructType(type));
  }

  @Test
  public void testGetValueNormal() throws Exception {
    new Expectations() {
      {
        request.getParameter("name");
        result = "v1,v2,v3";
      }
    };

    ParamValueProcessor processor = createProcessor("name", String.class);
    Object value = processor.getValue(request);
    Assert.assertEquals(value,"v1,v2,v3");
  }

  @Test
  public void testGetValueContainerType() throws Exception {
    new Expectations() {
      {
        request.getParameterValues("name");
        result = new String[] {"v1,v2,v3"};
      }
    };

    ParamValueProcessor processor = createProcessor("name", String[].class);
    String[] value = (String[]) processor.getValue(request);
    Assert.assertThat(value, Matchers.arrayContaining("v1","v2","v3"));
  }

  @Test
  public void testGetProcessorType() {
    ParamValueProcessor processor = createProcessor("name", String.class);
    Assert.assertEquals("query", processor.getProcessorType());
  }
}
