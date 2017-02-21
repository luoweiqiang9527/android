/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.testing;

import com.android.annotations.Nullable;
import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.android.AndroidTestBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.android.tools.idea.testing.FileSubject.file;
import static com.google.common.truth.Truth.assertAbout;
import static com.intellij.openapi.util.io.FileUtil.notNullize;

/**
 * Generates TestProjectPaths.
 */
public class TestProjectPathsGenerator {
  private static final List<String> TEST_SUB_FOLDERS =
    Arrays.asList("apk", "manifestConflict", "moduleInfo", "runConfig", "signapk", "sync", "testArtifacts", "uibuilder");

  public static void main(@NotNull String... args) throws IOException {
    TestProjectPathsInfo info = generateTestProjectPathsFile();

    System.out.println("About to write: ");
    System.out.println(info.fileContents);
    System.out.println("to " + info.javaFilePath.getPath());

    Files.write(Paths.get(info.javaFilePath.getPath()), info.fileContents.getBytes("UTF-8"));
  }

  @VisibleForTesting
  @NotNull
  static TestProjectPathsInfo generateTestProjectPathsFile() throws IOException {
    TestProjectPathsInfo testProjectPathsInfo = new TestProjectPathsInfo();

    File testDataFolderPath = new File(AndroidTestBase.getTestDataPath()).getCanonicalFile();
    assertAbout(file()).that(testDataFolderPath).isDirectory();

    File testProjectsFolderPath = new File(testDataFolderPath, "projects");
    assertAbout(file()).that(testProjectsFolderPath).isDirectory();

    File testFrameworkFolderPath = new File(new File(AndroidTestBase.getAndroidPluginHome()).getParentFile(), "android-test-framework");

    File testSrcFolderPath = new File(testFrameworkFolderPath, "testSrc").getCanonicalFile();
    assertAbout(file()).that(testSrcFolderPath).isDirectory();

    String packageName = TestProjectPathsGenerator.class.getPackage().getName();
    File parentFolderPath = new File(testSrcFolderPath, packageName.replace('.', File.separatorChar));

    String className = "TestProjectPaths";
    testProjectPathsInfo.javaFilePath = new File(parentFolderPath, className + ".java");

    StringBuilder buffer = new StringBuilder();
    buffer.append("package ").append(packageName).append(";\n\n");
    buffer.append("/**\n")
          .append(" * Do not edit. This class is generated by ").append(TestProjectPathsGenerator.class.getName()).append(".\n")
          .append(" */\n");
    buffer.append("public final class ").append(className).append(" {\n");

    File[] projectFolders = testProjectsFolderPath.listFiles();
    List<String> constants = new ArrayList<>();
    createConstants(constants, "", projectFolders);

    Collections.sort(constants);
    for (String constant : constants) {
      buffer.append(constant);
    }

    buffer.append("}");

    testProjectPathsInfo.fileContents = buffer.toString();
    return testProjectPathsInfo;
  }

  private static void createConstants(@NotNull List<String> constants, @NonNls @NotNull String prefix, @Nullable File[] projectFolders) {
    if (projectFolders == null) {
      return;
    }
    for (File projectFolder : projectFolders) {
      String projectName = projectFolder.getName();
      if (TEST_SUB_FOLDERS.contains(projectName)) {
        File[] subFolders = projectFolder.listFiles();
        createConstants(constants, projectName, subFolders);
      }
      else if ("navigator".equals(projectName)) {
        // Navigator is a special case where there isn't a pattern we can use recursively.
        addNavigatorProjects(constants, projectFolder);
      }
      else {
        String constantName = toConstantName(projectName);
        if (!prefix.isEmpty()) {
          constantName = toConstantName(prefix) + "_" + constantName;
        }
        addConstant(constants, prefix, projectName, constantName);
      }
    }
  }

  private static void addNavigatorProjects(@NotNull List<String> constants, @NotNull File projectFolder) {
    String rootFolderName = projectFolder.getName();
    String navigatorPrefix = toConstantName(rootFolderName);

    for (File subFolder : notNullize(projectFolder.listFiles())) {
      String subFolderName = subFolder.getName();
      if (subFolderName.equals("packageview")) {
        String packageviewPrefix = navigatorPrefix + "_" + toConstantName(subFolderName);
        // There are subprojects inside.
        for (File subProjectFolder : notNullize(subFolder.listFiles())) {
          String projectName = subProjectFolder.getName();
          String projectPath = rootFolderName + "/" + subFolderName + "/" + projectName;
          addConstant(constants, "", projectPath, packageviewPrefix + "_" + toConstantName(projectName));
        }
      }
      else {
        // This is a project
        String projectPath = rootFolderName + "/" + subFolderName;
        addConstant(constants, "", projectPath, navigatorPrefix + "_" + toConstantName(subFolderName));
      }
    }
  }

  private static void addConstant(@NotNull List<String> constants,
                                  @NotNull String prefix,
                                  @NotNull String projectPath,
                                  @NotNull String constantName) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("  public static final String ").append(constantName).append(" = \"projects/");
    if (!prefix.isEmpty()) {
      buffer.append(prefix).append("/");
    }
    buffer.append(projectPath).append("\";\n");
    constants.add(buffer.toString());
  }

  @NotNull
  private static String toConstantName(@NotNull String projectName) {
    StringBuilder buffer = new StringBuilder();

    char[] chars = projectName.toCharArray();
    for (char c : chars) {
      if (Character.isUpperCase(c)) {
        buffer.append("_");
      }
      buffer.append(Character.toUpperCase(c));
    }

    return buffer.toString();
  }

  @VisibleForTesting
  static class TestProjectPathsInfo {
    File javaFilePath;
    String fileContents;
  }
}
