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
package org.apache.servicecomb.it.schema;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Part;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

@RestSchema(schemaId = "uploadJaxrsSchema")
@Path("/v1/uploadJaxrsSchema")
public class UploadJaxrsSchema {
  private static final Logger LOGGER = LoggerFactory.getLogger(UploadJaxrsSchema.class);

  @Path("/upload1")
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public String fileUpload1(@FormParam("file1") Part file1, @FormParam("file2") Part file2) throws IOException {
    return getStrFromPart(file1) + getStrFromPart(file2);
  }

  @Path("/uploadArray1")
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public String uploadArray1(@FormParam("file1") Part[] file1, @FormParam("file2") Part file2) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < file1.length; i++) {
      stringBuilder.append(getStrFromPart(file1[i]));
    }
    return stringBuilder.append(getStrFromPart(file2)).toString();
  }

  @Path("/uploadList1")
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public String uploadList1(@FormParam("file1") List<Part> file1, @FormParam("file2") Part file2)
      throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    file1.forEach(part -> {
      stringBuilder.append(getStrFromPart(part));
    });
    return stringBuilder.append(getStrFromPart(file2)).toString();
  }

  @Path("/uploadArrayList1")
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public String uploadArrayList1(@FormParam("file1") ArrayList<Part> file1, @FormParam("file2") Part file2)
      throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    file1.forEach(part -> {
      stringBuilder.append(getStrFromPart(part));
    });
    return stringBuilder.append(getStrFromPart(file2)).toString();
  }

  @Path("/upload2")
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public String fileUpload2(@FormParam("file1") Part file1, @FormParam("message") String message) throws IOException {
    return getStrFromPart(file1) + message;
  }

  @Path("/uploadArray2")
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public String uploadArray2(@FormParam("file1") Part[] file1, @FormParam("message") String message)
      throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < file1.length; i++) {
      stringBuilder.append(getStrFromPart(file1[i]));
    }
    return stringBuilder.append(message).toString();
  }

  @Path("/uploadList2")
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public String uploadList2(@FormParam("file1") List<Part> file1, @FormParam("message") String message) {
    StringBuilder stringBuilder = new StringBuilder();
    file1.forEach(part -> stringBuilder.append(getStrFromPart(part)));
    return stringBuilder.append(message).toString();
  }

//  @Path("/uploadArrayList2")
//  @POST
//  @Produces(MediaType.TEXT_PLAIN)
//  public String uploadArrayList2(@FormParam("file1") ArrayList<Part> file1, @FormParam("message") String message) {
//    StringBuilder stringBuilder = new StringBuilder();
//    file1.forEach(part -> {
//      stringBuilder.append(getStrFromPart(part));
//    });
//    return stringBuilder.append(message).toString();
//  }

  @Path("/uploadMix")
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public String uploadMix(@FormParam("file1") List<Part> file1, @FormParam("file2") Part[] file2,
      @FormParam("message") String message) {
    StringBuilder stringBuilder = new StringBuilder();
    file1.forEach(part -> {
      stringBuilder.append(getStrFromPart(part));
    });
    for (int i = 0; i < file2.length; i++) {
      stringBuilder.append(getStrFromPart(file2[i]));
    }
    return stringBuilder.append(message).toString();
  }

  private static String getStrFromPart(Part file1) {
    String result;
    try (InputStream is1 = file1.getInputStream()) {
      result = IOUtils.toString(is1, "utf-8");
      LOGGER.info("get str from part {}={}={}", result, file1.getSubmittedFileName(), file1.getName());
    } catch (IOException e) {
      result = "e:" + e.getMessage();
    }
    return result;
  }

  @Path("/uploadMultiformMix")
  @POST
  public Map<String, String> uploadMultiformMix(@FormParam("file") MultipartFile file,
      @FormParam("fileList") List<MultipartFile> fileList,
      @FormParam("str") String str,
      @FormParam("strList") List<String> strList) throws IOException {
    HashMap<String, String> map = new HashMap<>();
    map.put("file", new String(file.getBytes(), StandardCharsets.UTF_8.name()));
    map.put("fileList", _fileUpload(fileList));
    map.put("str", str);
    map.put("strList", strList.toString());
    return map;
  }

  private static String _fileUpload(List<MultipartFile> fileList) {
    StringBuilder stringBuilder = new StringBuilder();
    try {
      for (MultipartFile multipartFile : fileList) {
        stringBuilder.append(IOUtils.toString(multipartFile.getBytes(), StandardCharsets.UTF_8.name()));
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    return stringBuilder.toString();
  }
}
