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
package com.android.tools.idea.insights.ui

import com.android.tools.idea.concurrency.AndroidDispatchers
import com.android.tools.idea.insights.AppInsightsProjectLevelControllerRule
import com.android.tools.idea.insights.EventPage
import com.android.tools.idea.insights.ISSUE1
import com.android.tools.idea.insights.ISSUE2
import com.android.tools.idea.insights.LoadingState
import com.android.tools.idea.insights.Permission
import com.android.tools.idea.insights.client.IssueResponse
import com.android.tools.idea.insights.waitForCondition
import com.android.tools.idea.projectsystem.PROJECT_SYSTEM_SYNC_TOPIC
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.testing.AndroidProjectRule
import com.google.common.truth.Truth.assertThat
import com.intellij.execution.filters.ExceptionFilters
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.impl.FoldingModelImpl
import com.intellij.openapi.util.Disposer
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class StackTraceConsoleTest {
  private val projectRule = AndroidProjectRule.inMemory()
  private val controllerRule = AppInsightsProjectLevelControllerRule(projectRule)

  private val fetchState =
    LoadingState.Ready(
      IssueResponse(listOf(ISSUE1, ISSUE2), emptyList(), emptyList(), emptyList(), Permission.FULL)
    )

  private lateinit var stackTraceConsole: StackTraceConsole

  @get:Rule val ruleChain: RuleChain = RuleChain.outerRule(projectRule).around(controllerRule)

  @Before
  fun setUp() {
    stackTraceConsole =
      runBlocking(AndroidDispatchers.uiThread) {
        StackTraceConsole(controllerRule.controller, projectRule.project, controllerRule.tracker)
          .apply {
            ExceptionFilters.getFilters(GlobalSearchScope.allScope(projectRule.project)).onEach {
              consoleView.addMessageFilter(it)
            }

            (consoleView.editor.foldingModel as FoldingModelImpl).isFoldingEnabled = false
          }
      }
    Disposer.register(controllerRule.disposable, stackTraceConsole)
  }

  @Test
  fun `when issue is selected, correct stack trace is printed`() = executeWithErrorProcessor {
    runBlocking(controllerRule.controller.coroutineScope.coroutineContext) {
      controllerRule.consumeInitialState(
        fetchState,
        eventsState = LoadingState.Ready(EventPage(listOf(ISSUE2.sampleEvent), ""))
      )
      WriteAction.run<RuntimeException>(stackTraceConsole.consoleView::flushDeferredText)
      stackTraceConsole.consoleView.waitAllRequests()

      assertThat(stackTraceConsole.consoleView.editor.document.text.trim())
        .isEqualTo(
          """
            javax.net.ssl.SSLHandshakeException: Trust anchor for certification path not found 
                com.android.org.conscrypt.SSLUtils.toSSLHandshakeException(SSLUtils.java:362)
                com.android.org.conscrypt.ConscryptEngine.convertException(ConscryptEngine.java:1134)
            Caused by: javax.net.ssl.SSLHandshakeException: Trust anchor for certification path not found 
                com.android.org.conscrypt.TrustManagerImpl.verifyChain(TrustManagerImpl.java:677)
                okhttp3.internal.connection.RealConnection.connectTls(RealConnection.java:320)
          """
            .trimIndent()
        )
    }
  }

  @Test
  fun `stack trace is re-highlighted after project sync`() {
    val file =
      projectRule.fixture.addFileToProject(
        "src/ResponseWrapper.kt",
        """
            package dev.firebase.appdistribution.api_service
            class ResponseWrapper {
              companion object
               {
                  fun build()
               }
            }
          """
          .trimIndent()
      )

    executeWithErrorProcessor {
      runBlocking(controllerRule.controller.coroutineScope.coroutineContext) {
        controllerRule.consumeInitialState(
          fetchState,
          eventsState = LoadingState.Ready(EventPage(listOf(ISSUE1.sampleEvent), ""))
        )

        WriteAction.run<RuntimeException>(stackTraceConsole.consoleView::flushDeferredText)
        stackTraceConsole.consoleView.waitAllRequests()

        // Ensure initial state: there's hyperlinks
        val hyperlinks = stackTraceConsole.consoleView.hyperlinks
        waitForCondition(6000) {
          // Below is what's printed out in the console:
          // ```
          //  retrofit2.HttpException: HTTP 401
          // dev.firebase.appdistribution.api_service.ResponseWrapper${'$'}Companion.build(ResponseWrapper.kt:23)
          // dev.firebase.appdistribution.api_service.ResponseWrapper${'$'}Companion.fetchOrError(ResponseWrapper.kt:31)
          // ```
          stackTraceConsole.consoleView.editor.document.lineCount > 0 &&
            hyperlinks.findAllHyperlinksOnLine(1).isNotEmpty() &&
            hyperlinks.findAllHyperlinksOnLine(2).isNotEmpty()
        }

        WriteAction.run<RuntimeException>(file::delete)
        // Sync and then the console will be re-highlighted: no hyperlinks anymore.
        projectRule.project.messageBus
          .syncPublisher(PROJECT_SYSTEM_SYNC_TOPIC)
          .syncEnded(ProjectSystemSyncManager.SyncResult.SUCCESS)
        dispatchAllInvocationEventsInIdeEventQueue()

        waitForCondition(6000) {
          hyperlinks.findAllHyperlinksOnLine(1).isEmpty() &&
            hyperlinks.findAllHyperlinksOnLine(2).isEmpty()
        }
      }
    }
  }
}
