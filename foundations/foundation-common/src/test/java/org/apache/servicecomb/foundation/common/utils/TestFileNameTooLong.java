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

package org.apache.servicecomb.foundation.common.utils;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestFileNameTooLong {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestFileNameTooLong.class);

  // ensure the maximum file path do not exceed. now set to 200(some old windows max 260)
  // smaller is better, can refactor in future
  // NOTICE: compiled classes or other generated files like surefire reports may exceed this size
  private static final int MAN_FILE_SIZE = 200;

  @Test
  public void assertFileNotTooLong() {
    File folder = new File(System.getProperty("user.dir"));
    LOGGER.error(folder.getAbsolutePath()); // $ROOT\foundations\foundation-common
    File root = new File(folder.getParentFile().getParent());
    LOGGER.error(root.getAbsolutePath()); // $ROOT\foundations\foundation-common
    Assert.assertTrue(root.exists());
    Assert.assertTrue(root.isDirectory());

    List<String> names = new LinkedList<>();
    findLongFileName(root, names, root.getAbsolutePath().length());
    Collections.sort(names);
    names.forEach(e -> LOGGER.error(e));
    if (!names.isEmpty()) {
      // for debug
      Assert.assertEquals("", names.toString());
    }
    Assert.assertTrue(names.isEmpty());
  }

  private static void findLongFileName(File folder, List<String> holder, int baseLenght) {
    if (folder.isFile()) {
      if (folder.getAbsolutePath().length() >= MAN_FILE_SIZE + baseLenght) {
        holder.add(folder.getAbsolutePath());
      }
    } else if (folder.isDirectory() && !"target".equals(folder.getName())) {
      File[] children = folder.listFiles();
      for (File child : children) {
        findLongFileName(child, holder, baseLenght);
      }
    } else {
      return;
    }
  }
}
