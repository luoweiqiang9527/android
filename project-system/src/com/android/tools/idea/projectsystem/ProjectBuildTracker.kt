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
package com.android.tools.idea.projectsystem

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker

/**
 * Service providing a [ModificationTracker] that updates on every project build completion.
 */
@Service(Service.Level.PROJECT)
class ProjectBuildTracker(val project: Project) : Disposable, ModificationTracker {
  private val tracker = SimpleModificationTracker()

  init {
    project.messageBus.connect(this).subscribe(PROJECT_SYSTEM_BUILD_TOPIC, object : ProjectSystemBuildManager.BuildListener {
      override fun buildCompleted(result: ProjectSystemBuildManager.BuildResult) {
        tracker.incModificationCount()
      }
    })
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project) = project.getService(ProjectBuildTracker::class.java)!!
  }

  override fun getModificationCount() = tracker.modificationCount
  override fun dispose() {}
}