/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.metrics.core;

public class EmbeddedMetricsName {
  private static final String APPLICATION_HEADER_FORMAT = "servicecomb.%s.application";

  public static final String APPLICATION_TOTAL_REQUEST_COUNT_PER_CONSUMER =
      APPLICATION_HEADER_FORMAT + ".requestCountPerConsumer.total";

  public static final String APPLICATION_TOTAL_REQUEST_COUNT_PER_PROVIDER =
      APPLICATION_HEADER_FORMAT + ".requestCountPerProvider.total";

  public static final String APPLICATION_FAILED_REQUEST_COUNT_PER_CONSUMER =
      APPLICATION_HEADER_FORMAT + ".requestCountPerConsumer.failed";

  public static final String APPLICATION_FAILED_REQUEST_COUNT_PER_PROVIDER =
      APPLICATION_HEADER_FORMAT + ".requestCountPerProvider.failed";

  private static final String QUEUE_HEADER_FORMAT = "servicecomb.%s.queue";

  public static final String QUEUE_COUNT_IN_QUEUE = QUEUE_HEADER_FORMAT + ".waitInQueue.count";

  public static final String QUEUE_EXECUTION_TIME = QUEUE_HEADER_FORMAT + ".executionTime";

  public static final String QUEUE_LIFE_TIME_IN_QUEUE = QUEUE_HEADER_FORMAT + ".lifeTimeInQueue";

  public static final String TPS_TOTAL_FORMAT = "servicecomb.%s.application.tps.total";

  public static final String TPS_FAILED_FORMAT = "servicecomb.%s.application.tps.failed";

  public static final String LATENCY_AVERAGE_FORMAT = "servicecomb.%s.application.latency.average";
}
