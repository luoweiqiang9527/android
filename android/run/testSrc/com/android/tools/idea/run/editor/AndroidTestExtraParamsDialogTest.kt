/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.run.editor

import com.android.tools.idea.gradle.project.sync.snapshots.AndroidCoreTestProject
import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.testing.EdtAndroidProjectRule
import com.android.tools.idea.testing.onEdt
import com.android.tools.idea.util.androidFacet
import com.google.common.truth.Truth.assertThat
import com.intellij.testFramework.RunsInEdt
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [AndroidTestExtraParamsDialog].
 */
class AndroidTestExtraParamsDialogTest {

  @get:Rule
  val projectRule: EdtAndroidProjectRule = AndroidProjectRule.testProject(AndroidCoreTestProject.RUN_CONFIG_RUNNER_ARGUMENTS).onEdt()

  val project get() = projectRule.project

  val androidFacet get() = projectRule.fixture.module.androidFacet!!


  @Test
  @RunsInEdt
  fun testAndroidTestExtraParamsDialog() {

    // RUN_CONFIG_RUNNER_ARGUMENTS test project defines two extra params in its Gradle build file as follows.
    assertThat(androidFacet.getAndroidTestExtraParams().toList()).containsExactly(
      AndroidTestExtraParam("size", "medium", "medium", AndroidTestExtraParamSource.GRADLE),
      AndroidTestExtraParam("foo", "bar", "bar", AndroidTestExtraParamSource.GRADLE))

    // Create dialog with includeGradleExtraParams true.
    var dialog = AndroidTestExtraParamsDialog(project, androidFacet, "")
    assertThat(dialog.instrumentationExtraParams).isEqualTo("-e size medium -e foo bar")
    assertThat(dialog.userModifiedInstrumentationExtraParams).isEqualTo("")
    dialog.close(0)

    // Supplying instrumentation extra params but dialog.userModifiedInstrumentationExtraParams still returns empty because those
    // supplied params are identical to Gradle build file.
    dialog = AndroidTestExtraParamsDialog(project, androidFacet, "-e foo bar -e size medium")
    assertThat(dialog.instrumentationExtraParams).isEqualTo("-e size medium -e foo bar")
    assertThat(dialog.userModifiedInstrumentationExtraParams).isEqualTo("")
    dialog.close(0)

    // Now override the value.
    dialog = AndroidTestExtraParamsDialog(project, androidFacet, "-e foo new_value -e size medium")
    assertThat(dialog.instrumentationExtraParams).isEqualTo("-e size medium -e foo new_value")
    assertThat(dialog.userModifiedInstrumentationExtraParams).isEqualTo("-e foo new_value")
    dialog.close(0)

    // Also supply additional value with new param name.
    dialog = AndroidTestExtraParamsDialog(project, androidFacet, "-e new_key and_value -e foo new_value -e size medium")
    assertThat(dialog.instrumentationExtraParams).isEqualTo("-e size medium -e foo new_value -e new_key and_value")
    assertThat(dialog.userModifiedInstrumentationExtraParams).isEqualTo("-e foo new_value -e new_key and_value")
    dialog.close(0)
  }
}
