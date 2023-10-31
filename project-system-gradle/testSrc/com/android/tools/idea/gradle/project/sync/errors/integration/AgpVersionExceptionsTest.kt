/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.sync.errors.integration

import com.android.ide.common.repository.AgpVersion
import com.android.tools.idea.gradle.project.sync.AgpVersionIncompatible
import com.android.tools.idea.gradle.project.sync.AgpVersionTooNew
import com.android.tools.idea.gradle.project.sync.AgpVersionTooOld
import com.android.tools.idea.gradle.project.sync.AgpVersionsMismatch
import com.android.tools.idea.gradle.project.sync.AndroidSyncException
import com.android.tools.idea.gradle.project.sync.IdeAndroidSyncError
import com.android.tools.idea.gradle.project.sync.SimulatedSyncErrors
import com.android.tools.idea.gradle.project.sync.quickFixes.OpenLinkQuickFix
import com.android.tools.idea.gradle.project.sync.snapshots.AndroidCoreTestProject
import com.android.tools.idea.gradle.project.sync.snapshots.TestProjectDefinition.Companion.prepareTestProject
import com.android.tools.idea.gradle.project.sync.toException
import com.google.common.truth.Truth
import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import org.junit.Test

class AgpVersionExceptionsTest : AbstractIssueCheckerIntegrationTest() {

  @Test
  fun testAgpVersionsMismatchError() {
    val preparedProject = projectRule.prepareTestProject(AndroidCoreTestProject.SIMPLE_APPLICATION)
    val originalException = AgpVersionsMismatch(
      listOf(
        Pair(AgpVersion.parse("7.2.1"), "mainProj"),
        Pair(AgpVersion.parse("7.4.0"), "included")
      )
    )

    SimulatedSyncErrors.registerSyncErrorToSimulate(originalException.simulatePassingThroughModel())

    runSyncAndCheckFailure(
      preparedProject,
      { buildIssue ->
        Truth.assertThat(buildIssue).isNotNull()
        Truth.assertThat(buildIssue.description).contains("""
          Using multiple versions of the Android Gradle Plugin [7.2.1, 7.4.0] across Gradle builds is not allowed.
          Affected builds: [mainProj, included].
        """.trimIndent()
        )
        Truth.assertThat(buildIssue.quickFixes).hasSize(0)
      },
      AndroidStudioEvent.GradleSyncFailure.MULTIPLE_ANDROID_PLUGIN_VERSIONS
    )
  }

  @Test
  fun testAgpVersionTooOldError() {
    val preparedProject = projectRule.prepareTestProject(AndroidCoreTestProject.SIMPLE_APPLICATION)
    val originalException = AgpVersionTooOld(AgpVersion.parse("3.1.4"))

    SimulatedSyncErrors.registerSyncErrorToSimulate(originalException.simulatePassingThroughModel())

    runSyncAndCheckFailure(
      preparedProject,
      { buildIssue ->
        Truth.assertThat(buildIssue).isNotNull()
        Truth.assertThat(buildIssue.quickFixes.size).isEqualTo(1)
        Truth.assertThat(buildIssue.description)
          .contains("The project is using an incompatible version (AGP 3.1.4) of the Android Gradle plugin.")
        Truth.assertThat(buildIssue.quickFixes[0]).isInstanceOf(OpenLinkQuickFix::class.java)
        Truth.assertThat((buildIssue.quickFixes[0] as OpenLinkQuickFix).link)
          .isEqualTo("https://developer.android.com/studio/releases#android_gradle_plugin_and_android_studio_compatibility")
      },
      AndroidStudioEvent.GradleSyncFailure.OLD_ANDROID_PLUGIN
    )
  }

  @Test
  fun testAgpVersionTooNewError() {
    val preparedProject = projectRule.prepareTestProject(AndroidCoreTestProject.SIMPLE_APPLICATION)
    val originalException = AgpVersionTooNew(AgpVersion.parse("99.1.4"))

    SimulatedSyncErrors.registerSyncErrorToSimulate(originalException.simulatePassingThroughModel())

    runSyncAndCheckFailure(
      preparedProject,
      { buildIssue ->
        Truth.assertThat(buildIssue).isNotNull()
        Truth.assertThat(buildIssue.quickFixes.size).isEqualTo(1)
        Truth.assertThat(buildIssue.description)
          .contains("The project is using an incompatible version (AGP 99.1.4) of the Android Gradle plugin.")
        Truth.assertThat(buildIssue.quickFixes[0]).isInstanceOf(OpenLinkQuickFix::class.java)
        Truth.assertThat((buildIssue.quickFixes[0] as OpenLinkQuickFix).link)
          .isEqualTo("https://developer.android.com/studio/releases#android_gradle_plugin_and_android_studio_compatibility")
      },
      AndroidStudioEvent.GradleSyncFailure.ANDROID_PLUGIN_TOO_NEW
    )
  }

  @Test
  fun testAgpVersionIncompatible() {
    val preparedProject = projectRule.prepareTestProject(AndroidCoreTestProject.SIMPLE_APPLICATION)
    val originalException = AgpVersionIncompatible(AgpVersion.parse("7.1.0-beta01"))

    SimulatedSyncErrors.registerSyncErrorToSimulate(originalException.simulatePassingThroughModel())

    runSyncAndCheckFailure(
      preparedProject,
      { buildIssue ->
        Truth.assertThat(buildIssue).isNotNull()
        Truth.assertThat(buildIssue.quickFixes.size).isEqualTo(1)
        Truth.assertThat(buildIssue.description)
          .contains("The project is using an incompatible preview version (AGP 7.1.0-beta01) of the Android Gradle plugin.")
        Truth.assertThat(buildIssue.quickFixes[0]).isInstanceOf(OpenLinkQuickFix::class.java)
        Truth.assertThat((buildIssue.quickFixes[0] as OpenLinkQuickFix).link)
          .isEqualTo("https://developer.android.com/studio/preview/features#agp-previews")
      },
      AndroidStudioEvent.GradleSyncFailure.ANDROID_PLUGIN_VERSION_INCOMPATIBLE
    )
  }

  private fun AndroidSyncException.simulatePassingThroughModel(): AndroidSyncException = IdeAndroidSyncError(
    message = message.orEmpty(),
    stackTrace = stackTrace.map { it.toString() },
    buildPath = myBuildPath,
    modulePath = myModulePath,
    syncIssues = mySyncIssues
  ).toException()
}