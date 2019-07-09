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
package com.android.build.attribution

import com.android.build.attribution.analyzers.BuildEventsAnalyzersProxy
import com.android.build.attribution.analyzers.BuildEventsAnalyzersWrapper
import com.google.common.annotations.VisibleForTesting
import com.intellij.build.BuildContentManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.gradle.tooling.events.ProgressEvent
import java.time.Duration

class BuildAttributionManagerImpl(
  private val myProject: Project,
  private val myBuildContentManager: BuildContentManager
) : BuildAttributionManager {
  @get:VisibleForTesting
  val analyzersProxy = BuildEventsAnalyzersProxy()
  private val analyzersWrapper = BuildEventsAnalyzersWrapper(analyzersProxy.getAnalyzers())

  override fun onBuildStart() {
    analyzersWrapper.onBuildStart()
  }

  override fun onBuildSuccess() {
    analyzersWrapper.onBuildSuccess()

    // TODO: add proper UI
    logBuildAttributionResults()
  }

  override fun onBuildFailure() {
    analyzersWrapper.onBuildFailure()
  }

  override fun statusChanged(event: ProgressEvent?) {
    if (event == null) return

    analyzersWrapper.receiveEvent(event)
  }

  private fun logBuildAttributionResults() {
    val stringBuilder = StringBuilder()

    analyzersProxy.getNonIncrementalAnnotationProcessorsData().let {
      if (it.isNotEmpty()) {
        stringBuilder.appendln("Non incremental annotation processors:")
        it.forEach { processor -> stringBuilder.appendln(processor.className + " " + processor.compilationDuration) }
      }
    }

    analyzersProxy.getTasksCriticalPath().let {
      if (it.isNotEmpty()) {
        stringBuilder.appendln("Tasks critical path:")
        it.forEach { taskBuildData ->
          val percentage = taskBuildData.taskExecutionTime * 100 / analyzersProxy.getCriticalPathDuration()
          stringBuilder.append("Task ${taskBuildData.taskData.getTaskPath()} from ${taskBuildData.taskData.originPlugin}")
            .appendln(", time ${Duration.ofMillis(taskBuildData.taskExecutionTime)} ($percentage%)")
        }
      }
    }

    analyzersProxy.getPluginsCriticalPath().let {
      if (it.isNotEmpty()) {
        stringBuilder.appendln("Plugins determining build duration:")
        it.forEach { pluginBuildData ->
          val percentage = pluginBuildData.buildDuration * 100 / analyzersProxy.getCriticalPathDuration()
          stringBuilder.append("${pluginBuildData.plugin}, time ${Duration.ofMillis(pluginBuildData.buildDuration)}")
            .appendln(" ($percentage%)")
        }
      }
    }

    if (stringBuilder.isNotEmpty()) {
      Logger.getInstance(this::class.java).warn("Build attribution analysis results:\n$stringBuilder")
    }
  }

}