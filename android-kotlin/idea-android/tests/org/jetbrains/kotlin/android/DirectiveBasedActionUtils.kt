/*
 * Copyright (C) 2021 The Android Open Source Project
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
package org.jetbrains.kotlin.android

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiFile
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KtDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

// Adapted from the Kotlin test framework (after taking over android-kotlin sources).
object DirectiveBasedActionUtils {

  private fun checkForUnexpectedErrorsBase(
    file: KtFile,
    diagnosticsCollectAndRenderer: (KtFile) -> Collection<String>,
  ) {
    if (InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.text, "// DISABLE-ERRORS").isNotEmpty()) {
      return
    }

    val expected = InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.text, "// ERROR:").sorted()
    val actual = diagnosticsCollectAndRenderer(file)

    UsefulTestCase.assertOrderedEquals(
      "All actual errors should be mentioned in test data with '// ERROR:' directive. But no unnecessary errors should be mentioned",
      actual, expected
    )
  }

  fun checkForUnexpectedErrorsK1(file: KtFile, diagnosticsProvider: (KtFile) -> Diagnostics = { it.analyzeWithContent().diagnostics }) {
    checkForUnexpectedErrorsBase(
      file,
    ) { ktFile ->
      diagnosticsProvider(ktFile)
        .filter { it.severity == Severity.ERROR }
        .map { DefaultErrorMessages.render(it).replace("\n", "<br>") }
        .sorted()
    }
  }

  @OptIn(KtAllowAnalysisOnEdt::class)
  fun checkForUnexpectedErrorsK2(file: KtFile) {
    checkForUnexpectedErrorsBase(
      file,
    ) { ktFile ->
      allowAnalysisOnEdt {
        analyze(ktFile) {
          ktFile.collectDiagnosticsForFile(KtDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
            .filter { it.severity == Severity.ERROR }
            .map { it.defaultMessage.replace("\n", "<br>") }
            .sorted()
        }
      }
    }
  }

  fun checkAvailableActionsAreExpected(file: PsiFile, availableActions: Collection<IntentionAction>) {
    if (InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.text, "// IGNORE_IRRELEVANT_ACTIONS").isNotEmpty()) {
      return
    }

    val expectedActions = InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.text, "// ACTION:").sorted()
    val actualActions = availableActions.map { it.text }.sorted()

    UsefulTestCase.assertOrderedEquals(
      "Some unexpected actions available at current position. Use // ACTION: directive",
      actualActions, expectedActions
    )
  }
}
