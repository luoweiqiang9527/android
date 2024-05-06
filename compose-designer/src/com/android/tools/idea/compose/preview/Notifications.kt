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
package com.android.tools.idea.compose.preview

import com.android.tools.idea.editors.sourcecode.isKotlinFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import java.util.concurrent.TimeUnit

/**
 * [ProjectComponent] that listens for Kotlin file additions or removals and triggers a notification
 * update
 */
@Service(Service.Level.PROJECT)
internal class ComposeNewPreviewNotificationManager : Disposable {
  private val LOG = Logger.getInstance(ComposeNewPreviewNotificationManager::class.java)

  private val updateNotificationQueue: MergingUpdateQueue by lazy {
    MergingUpdateQueue(
      "Update notifications",
      TimeUnit.SECONDS.toMillis(2).toInt(),
      true,
      null,
      this
    )
  }

  override fun dispose() {}

  class MyStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
      project.service<ComposeNewPreviewNotificationManager>().projectOpened(project)
    }
  }

  private fun projectOpened(project: Project) {
    LOG.debug("projectOpened")

    PsiManager.getInstance(project)
      .addPsiTreeChangeListener(
        object : PsiTreeChangeAdapter() {
          private fun onEvent(event: PsiTreeChangeEvent) {
            val file = event.file?.virtualFile ?: return
            if (!file.isKotlinFileType()) return
            updateNotificationQueue.queue(
              object : Update(file) {
                override fun run() {
                  if (project.isDisposed || !file.isValid) {
                    return
                  }

                  if (LOG.isDebugEnabled) {
                    LOG.debug("updateNotifications for ${file.name}")
                  }

                  if (FileEditorManager.getInstance(project).getEditors(file).isEmpty()) {
                    LOG.debug("No editor found")
                    return
                  }

                  EditorNotifications.getInstance(project).updateNotifications(file)
                }
              }
            )
          }

          override fun childAdded(event: PsiTreeChangeEvent) {
            onEvent(event)
          }

          override fun childRemoved(event: PsiTreeChangeEvent) {
            onEvent(event)
          }
        },
        this
      )
  }
}
