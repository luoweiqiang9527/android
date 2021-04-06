/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.tools.idea.sdk;

import static com.android.testutils.TestUtils.getSdk;
import static com.android.tools.idea.testing.AndroidGradleTests.getEmbeddedJdk8Path;
import static com.android.tools.idea.testing.Facets.createAndAddAndroidFacet;
import static com.android.tools.idea.testing.Facets.createAndAddGradleFacet;
import static com.google.common.truth.Truth.assertThat;
import static com.intellij.openapi.projectRoots.JavaSdkVersion.JDK_11;
import static com.intellij.openapi.projectRoots.JavaSdkVersion.JDK_12;
import static com.intellij.openapi.projectRoots.JavaSdkVersion.JDK_14;
import static com.intellij.openapi.projectRoots.JavaSdkVersion.JDK_1_7;
import static com.intellij.openapi.projectRoots.JavaSdkVersion.JDK_1_8;
import static com.intellij.openapi.projectRoots.JavaSdkVersion.JDK_1_9;
import static com.intellij.openapi.util.io.FileUtil.filesEqual;
import static com.intellij.openapi.util.io.FileUtil.toSystemDependentName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.android.repository.api.LocalPackage;
import com.android.repository.api.RepoManager;
import com.android.repository.impl.meta.RepositoryPackages;
import com.android.repository.io.FileOp;
import com.android.repository.io.FileOpUtils;
import com.android.repository.testframework.FakePackage;
import com.android.repository.testframework.FakeRepoManager;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.tools.idea.AndroidTestCaseHelper;
import com.android.tools.idea.IdeInfo;
import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.gradle.project.AndroidGradleProjectSettingsControlBuilder;
import com.android.tools.idea.gradle.util.EmbeddedDistributionPaths;
import com.android.tools.idea.gradle.util.LocalProperties;
import com.android.tools.idea.testing.IdeComponents;
import com.android.tools.idea.testing.Sdks;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.PlatformTestCase;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.sdk.AndroidPlatform;
import org.jetbrains.android.sdk.AndroidSdkData;
import org.jetbrains.annotations.NotNull;

/**
 * Tests for {@link IdeSdks}.
 */
public class IdeSdksTest extends PlatformTestCase {
  private IdeInfo myIdeInfo;

  private File myAndroidSdkPath;
  private EmbeddedDistributionPaths myEmbeddedDistributionPaths;
  private IdeSdks myIdeSdks;
  private AndroidSdks myAndroidSdks;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    initMocks(this);
    myIdeInfo = IdeInfo.getInstance();

    AndroidTestCaseHelper.removeExistingAndroidSdks();
    myAndroidSdkPath = getSdk().toFile();

    AndroidFacet facet = createAndAddAndroidFacet(myModule);
    facet.getProperties().ALLOW_USER_CONFIGURATION = false;

    createAndAddGradleFacet(myModule);

    Jdks jdks = new Jdks(myIdeInfo);
    myEmbeddedDistributionPaths = EmbeddedDistributionPaths.getInstance();
    myAndroidSdks = new AndroidSdks(myIdeInfo);
    myIdeSdks = new IdeSdks(myAndroidSdks, jdks, myEmbeddedDistributionPaths, myIdeInfo);
    IdeSdks.removeJdksOn(getTestRootDisposable());
    Sdks.allowAccessToSdk(getTestRootDisposable());
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      myAndroidSdks = null;
      myIdeSdks = null;
      AndroidTestCaseHelper.removeExistingAndroidSdks();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  public void testCreateAndroidSdkPerAndroidTarget() {
    List<Sdk> sdks = myIdeSdks.createAndroidSdkPerAndroidTarget(myAndroidSdkPath);
    assertOneSdkPerAvailableTarget(sdks);
  }

  public void testGetAndroidSdkPath() {
    // Create default SDKs first.
    myIdeSdks.createAndroidSdkPerAndroidTarget(myAndroidSdkPath);

    File androidHome = myIdeSdks.getAndroidSdkPath();
    assertNotNull(androidHome);
    assertEquals(myAndroidSdkPath.getPath(), androidHome.getPath());
  }

  public void testGetAndroidNdkPath() {
    FileOp fop = FileOpUtils.create();
    FakePackage.FakeLocalPackage value = new FakePackage.FakeLocalPackage("ndk;21.0.0", fop);
    setupSdkData(ImmutableList.of(value), fop);

    File ndkPath = myIdeSdks.getAndroidNdkPath();
    String osPrefix = SystemInfo.isWindows ? "C:" : "";
    assertThat(ndkPath.getAbsolutePath())
      .matches(osPrefix + Pattern.quote(toSystemDependentName("/sdk/ndk/")) + "[0-9.]+");
  }

  public void testGetAndroidNdkPathWithPredicate() {
    FileOp fop = FileOpUtils.create();
    FakePackage.FakeLocalPackage value = new FakePackage.FakeLocalPackage("ndk;21.0.0", fop);
    setupSdkData(ImmutableList.of(value), fop);

    File ndkPath = myIdeSdks.getAndroidNdkPath(revision -> false);
    assertThat(ndkPath).isNull();
  }

  public void testGetEligibleAndroidSdks() {
    // Create default SDKs first.
    List<Sdk> sdks = myIdeSdks.createAndroidSdkPerAndroidTarget(myAndroidSdkPath);

    List<Sdk> eligibleSdks = myIdeSdks.getEligibleAndroidSdks();
    assertEquals(sdks.size(), eligibleSdks.size());
  }

  public void testSetAndroidSdkPathUpdatingLocalPropertiesFile() throws IOException {
    LocalProperties localProperties = new LocalProperties(myProject);
    localProperties.setAndroidSdkPath("");
    localProperties.save();

    List<Sdk> sdks =
      ApplicationManager.getApplication().runWriteAction((Computable<List<Sdk>>)() -> myIdeSdks.setAndroidSdkPath(myAndroidSdkPath, null));
    assertOneSdkPerAvailableTarget(sdks);

    localProperties = new LocalProperties(myProject);
    File androidSdkPath = localProperties.getAndroidSdkPath();
    assertNotNull(androidSdkPath);
    assertEquals(myAndroidSdkPath.getPath(), androidSdkPath.getPath());
  }

  private void assertOneSdkPerAvailableTarget(@NotNull List<Sdk> sdks) {
    List<IAndroidTarget> platformTargets = Lists.newArrayList();
    AndroidSdkData sdkData = AndroidSdkData.getSdkData(myAndroidSdkPath);
    assertNotNull(sdkData);
    for (IAndroidTarget target : sdkData.getTargets()) {
      if (target.isPlatform()) {
        platformTargets.add(target);
      }
    }

    assertEquals(platformTargets.size(), sdks.size());

    for (Sdk sdk : sdks) {
      AndroidPlatform androidPlatform = AndroidPlatform.getInstance(sdk);
      assertNotNull(androidPlatform);
      IAndroidTarget target = androidPlatform.getTarget();
      platformTargets.remove(target);
    }

    assertEquals(0, platformTargets.size());
  }

  public void testUseEmbeddedJdk() {
    if (!myIdeInfo.isAndroidStudio()) {
      return; // Idea does not have embedded JDK. Skip this test.
    }
    ApplicationManager.getApplication().runWriteAction(() -> myIdeSdks.setUseEmbeddedJdk());

    // The path of the JDK should be the same as the embedded one.
    File jdkPath = myIdeSdks.getJdkPath();
    assertNotNull(jdkPath);

    File embeddedJdkPath = myEmbeddedDistributionPaths.getEmbeddedJdkPath();
    assertTrue(String.format("'%1$s' should be the embedded one ('%2$s')", jdkPath.getPath(), embeddedJdkPath.getPath()),
               filesEqual(jdkPath, embeddedJdkPath));
  }

  public void testIsJavaSameVersionNull() {
    assertFalse(IdeSdks.isJdkSameVersion(null, JDK_1_8));
  }

  public void testIsJavaSameVersionTrue() {
    Jdks spyJdks = spy(Jdks.getInstance());
    new IdeComponents(myProject).replaceApplicationService(Jdks.class, spyJdks);
    File fakeFile = new File(myProject.getBasePath());
    doReturn(JDK_1_8).when(spyJdks).findVersion(same(fakeFile));
    assertTrue(IdeSdks.isJdkSameVersion(fakeFile, JDK_1_8));
  }

  public void testIsJavaSameVersionLower() {
    Jdks spyJdks = spy(Jdks.getInstance());
    new IdeComponents(myProject).replaceApplicationService(Jdks.class, spyJdks);
    File fakeFile = new File(myProject.getBasePath());
    doReturn(JDK_1_7).when(spyJdks).findVersion(same(fakeFile));
    assertFalse(IdeSdks.isJdkSameVersion(fakeFile, JDK_1_8));
  }

  public void testIsJavaSameVersionHigher() {
    Jdks spyJdks = spy(Jdks.getInstance());
    new IdeComponents(myProject).replaceApplicationService(Jdks.class, spyJdks);
    File fakeFile = new File(myProject.getBasePath());
    doReturn(JDK_1_9).when(spyJdks).findVersion(same(fakeFile));
    assertFalse(IdeSdks.isJdkSameVersion(fakeFile, JDK_1_8));
  }

  public void testJdkEnvVariableNotDefined() {
    myIdeSdks.overrideJdkEnvVariable(null);
    assertThat(myIdeSdks.isJdkEnvVariableDefined()).isFalse();
    assertThat(myIdeSdks.isJdkEnvVariableValid()).isFalse();
    assertThat(myIdeSdks.getEnvVariableJdkFile()).isNull();
    assertThat(myIdeSdks.getEnvVariableJdkValue()).isNull();
    assertThat(myIdeSdks.isUsingEnvVariableJdk()).isFalse();
    assertThat(myIdeSdks.setUseEnvVariableJdk(true)).isFalse();
    assertThat(myIdeSdks.isUsingEnvVariableJdk()).isFalse();
  }

  public void testJdkEnvVariableNotValid() {
    String invalidPath = "not_a_valid_path";
    myIdeSdks.overrideJdkEnvVariable(invalidPath);
    assertThat(myIdeSdks.isJdkEnvVariableDefined()).isTrue();
    assertThat(myIdeSdks.isJdkEnvVariableValid()).isFalse();
    assertThat(myIdeSdks.getEnvVariableJdkFile()).isNull();
    assertThat(myIdeSdks.getEnvVariableJdkValue()).isEqualTo(invalidPath);
    assertThat(myIdeSdks.isUsingEnvVariableJdk()).isFalse();
    assertThat(myIdeSdks.setUseEnvVariableJdk(true)).isFalse();
    assertThat(myIdeSdks.isUsingEnvVariableJdk()).isFalse();
  }

  public void testJdkEnvVariableValid() {
    String validPath = IdeSdks.getJdkFromJavaHome();
    myIdeSdks.overrideJdkEnvVariable(validPath);
    assertThat(myIdeSdks.isJdkEnvVariableDefined()).isTrue();
    assertThat(myIdeSdks.isJdkEnvVariableValid()).isTrue();
    assertThat(myIdeSdks.getEnvVariableJdkFile()).isEqualTo(new File(validPath));
    assertThat(myIdeSdks.getEnvVariableJdkValue()).isEqualTo(validPath);
    assertThat(myIdeSdks.isUsingEnvVariableJdk()).isTrue();
    assertThat(myIdeSdks.setUseEnvVariableJdk(false)).isTrue();
    assertThat(myIdeSdks.isUsingEnvVariableJdk()).isFalse();
    assertThat(myIdeSdks.setUseEnvVariableJdk(true)).isTrue();
    assertThat(myIdeSdks.isUsingEnvVariableJdk()).isTrue();
    File expectedFile = new File(toSystemDependentName(validPath));
    File jdkFile = new File(toSystemDependentName(myIdeSdks.getJdk().getHomePath()));
    assertThat(jdkFile).isEqualTo(expectedFile);
  }

  private void setupSdkData(ImmutableList<LocalPackage> localPackages, FileOp fop) {
    RepositoryPackages packages = new RepositoryPackages(localPackages, Collections.emptyList());
    RepoManager repoManager = new FakeRepoManager(packages);
    AndroidSdkHandler androidSdkHandler = new AndroidSdkHandler(null, null, fop, repoManager);
    AndroidSdkData androidSdkData = mock(AndroidSdkData.class);
    doReturn(androidSdkHandler).when(androidSdkData).getSdkHandler();
    myAndroidSdks.setSdkData(androidSdkData);
  }

  public void testIsJdkVersionCompatible() {
    assertThat(myIdeSdks.isJdkVersionCompatible(JDK_1_8, JDK_1_7)).isFalse();
    assertThat(myIdeSdks.isJdkVersionCompatible(JDK_1_8, JDK_1_8)).isTrue();
    assertThat(myIdeSdks.isJdkVersionCompatible(JDK_1_8, JDK_1_9)).isTrue();
    assertThat(myIdeSdks.isJdkVersionCompatible(JDK_1_8, JDK_11)).isTrue();
    assertThat(myIdeSdks.isJdkVersionCompatible(JDK_1_8, JDK_12)).isFalse();
    assertThat(myIdeSdks.isJdkVersionCompatible(JDK_1_8, JDK_14)).isFalse();
  }

  public void testExistingJdkIsNotDuplicated() {
    Jdks spyJdks = spy(Jdks.getInstance());
    new IdeComponents(myProject).replaceApplicationService(Jdks.class, spyJdks);
    IdeSdks ideSdks = IdeSdks.getInstance();
    Sdk currentJdk = ideSdks.getJdk();
    assertThat(currentJdk).isNotNull();
    String homePath = currentJdk.getHomePath();
    assertThat(homePath).isNotNull();
    assertThat(homePath).isNotEqualTo("");
    AtomicReference<Sdk> newJdk = new AtomicReference<>(null);
    ApplicationManager.getApplication().runWriteAction(() -> {
      ideSdks.overrideJdkEnvVariable(homePath);
      newJdk.set(ideSdks.getJdk());
    });
    verify(spyJdks, never()).createJdk(any());
    assertThat(newJdk.get()).isSameAs(currentJdk);
  }

  /**
   * Confirm that the default JDK is used when it is in the JDK table
   */
  public void testDefaultJdkIsUsed() throws IOException {
    // Only test if per project JDK is enabled
    if (!StudioFlags.ALLOW_JDK_PER_PROJECT.get()) {
      return;
    }
    Sdk currentJdk = myIdeSdks.getJdk();
    assertThat(currentJdk).isNotNull();
    String homePath = currentJdk.getHomePath();
    assertThat(homePath).isNotNull();
    assertThat(homePath).isNotEqualTo("");

    Sdk jdk8 = IdeSdks.findOrCreateJdk(AndroidGradleProjectSettingsControlBuilder.ANDROID_STUDIO_DEFAULT_JDK_NAME,
                                       new File(getEmbeddedJdk8Path()));
    assertThat(jdk8).isNotNull();
    Sdk newJdk = myIdeSdks.getJdk();
    assertThat(newJdk).isSameAs(jdk8);
  }
}
