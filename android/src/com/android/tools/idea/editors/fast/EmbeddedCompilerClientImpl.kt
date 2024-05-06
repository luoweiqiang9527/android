/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.tools.idea.editors.fast

import com.android.tools.idea.concurrency.AndroidDispatchers
import com.android.tools.idea.editors.liveedit.LiveEditService
import com.android.tools.idea.run.deployment.liveedit.LiveEditUpdateException
import com.android.tools.idea.run.deployment.liveedit.analyzeSingleDepthInlinedFunctions
import com.android.tools.idea.run.deployment.liveedit.isKotlinPluginBundled
import com.android.tools.idea.run.deployment.liveedit.runWithCompileLock
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressWrapper
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.io.createParentDirectories
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createFile

private fun Throwable?.isCompilationError(): Boolean =
  this is LiveEditUpdateException
  && when (error) {
    LiveEditUpdateException.Error.ANALYSIS_ERROR -> message?.startsWith("Analyze Error.") ?: false
    LiveEditUpdateException.Error.COMPILATION_ERROR -> true
    LiveEditUpdateException.Error.UNABLE_TO_INLINE,
    LiveEditUpdateException.Error.NON_KOTLIN,
    LiveEditUpdateException.Error.NON_PRIVATE_INLINE_FUNCTION,
    LiveEditUpdateException.Error.INTERNAL_ERROR,
    LiveEditUpdateException.Error.INTERNAL_ERROR_NO_BINDING_CONTEXT,
    LiveEditUpdateException.Error.UNABLE_TO_LOCATE_COMPOSE_GROUP,
    LiveEditUpdateException.Error.UNSUPPORTED_SRC_CHANGE_RECOVERABLE,
    LiveEditUpdateException.Error.UNSUPPORTED_SRC_CHANGE_UNRECOVERABLE,
    LiveEditUpdateException.Error.UNSUPPORTED_BUILD_SRC_CHANGE,
    LiveEditUpdateException.Error.UNSUPPORTED_TEST_SRC_CHANGE,
    LiveEditUpdateException.Error.UNABLE_TO_DESUGAR,
    LiveEditUpdateException.Error.UNSUPPORTED_BUILD_LIBRARY_DESUGAR,
    LiveEditUpdateException.Error.BAD_MIN_API,
    LiveEditUpdateException.Error.KNOWN_ISSUE -> false
  }

/**
 * [Throwable] used by the [EmbeddedCompilerClientImpl] during a compilation when the embedded plugin is not being used. This signals
 * to the caller that the error *might* have been caused by an incompatibility of the Kotlin Plugin.
 */
private class NotUsingKotlinBundledPlugin(cause: Throwable):
  Exception(FastPreviewBundle.message("fast.preview.error.needs.bundled.plugin"), cause)

/**
 * Implementation of the [CompilerDaemonClient] that uses the embedded compiler in Android Studio. This allows
 * to compile fragments of code in-process similar to how Live Edit does for the emulator.
 *
 * [isKotlinPluginBundled] is a method that returns if the available Kotlin Plugin is the version bundled with Android Studio. This
 * is used to diagnose problems and inform users when there is a failure that might have been caused by the user updating the Kotlin
 * Plugin.
 *
 * [beforeCompilationStarts] can be used during testing to trigger error conditions, by default in production, it does nothing.
 */
class EmbeddedCompilerClientImpl private constructor(
  private val project: Project,
  private val log: Logger,
  private val isKotlinPluginBundled: () -> Boolean,
  private val beforeCompilationStarts: suspend () -> Unit) : CompilerDaemonClient {

  constructor(project: Project, log: Logger):
    this(project, log, ::isKotlinPluginBundled, {})

  @TestOnly
  constructor(project: Project,
              log: Logger,
              isKotlinPluginBundled: Boolean = true,
              beforeCompilationStarts: suspend () -> Unit = {}) :
    this(project, log,
         isKotlinPluginBundled = { isKotlinPluginBundled },
         beforeCompilationStarts = beforeCompilationStarts)

  private val daemonLock = Mutex()
  override val isRunning: Boolean
    get() = daemonLock.holdsLock(this)

  /**
   * The Live Edit inline candidates cache. The cache can only be accessed with the Compile lock (see [runWithCompileLock]).
   * The cache is automatically invalidated on build.
   */
  private val inlineCandidateCache = LiveEditService.getInstance(project).inlineCandidateCache()

  /**
   * Compiles the given list of inputs using [module] as context. The output will be generated in the given [outputDirectory] and progress
   * will be updated in the given [ProgressIndicator].
   */
  private suspend fun compileKtFiles(inputs: List<KtFile>, module: Module, outputDirectory: Path) = withContext(AndroidDispatchers.workerThread) {
    log.debug("compileKtFile($inputs, $outputDirectory)")

    val generationState =
      readAction {
        runWithCompileLock {
          beforeCompilationStarts()

          log.debug("fetchResolution")
          val resolution = fetchResolution(project, inputs)
          ProgressManager.checkCanceled()
          log.debug("analyze")
          val analysisResult = analyze(inputs, resolution)
          val inlineCandidates = inputs
            .flatMap { analyzeSingleDepthInlinedFunctions(it, analysisResult.bindingContext, inlineCandidateCache) }
            .toSet()
          ProgressManager.checkCanceled()
          log.debug("backCodeGen")
          try {
            backendCodeGen(project, analysisResult, inputs, module, inlineCandidates)
          }
          catch (e: LiveEditUpdateException) {
            if (e.isCompilationError() || e.cause.isCompilationError()) {
              log.debug("backCodeGen compilation exception ", e)
              throw e
            }

            if (e.error != LiveEditUpdateException.Error.UNABLE_TO_INLINE) {
              log.debug("backCodeGen exception ", e)
              throw e
            }

            // Add any extra source file this compilation need in order to support the input file calling an inline function
            // from another source file then perform a compilation again.
            log.debug("inline analysis")
            val inputFilesWithInlines = inputs.flatMap {
              performInlineSourceDependencyAnalysis(resolution, it, analysisResult.bindingContext)
            }

            // We need to perform the analysis once more with the new set of input files.
            log.debug("inline analysis with inlines ${inputFilesWithInlines.joinToString(",") { it.name }}")
            val newAnalysisResult = resolution.analyzeWithAllCompilerChecks(inputFilesWithInlines)

            // We will need to start using the new analysis for code gen.
            log.debug("backCodeGen retry")
            backendCodeGen(project, newAnalysisResult, inputFilesWithInlines, module, inlineCandidates)
          }
        }
      }
    log.debug("backCodeGen completed")

    generationState.factory.asList().forEach {
      log.debug("output: ${it.relativePath}")
      val path = outputDirectory.resolve(it.relativePath)
      path.createParentDirectories().createFile()
      Files.write(path, it.asByteArray())
    }
  }

  override suspend fun compileRequest(
    files: Collection<PsiFile>,
    module: Module,
    outputDirectory: Path,
    indicator: ProgressIndicator): CompilationResult = coroutineScope {
    daemonLock.withLock(this) {
      val inputs = files.filterIsInstance<KtFile>().toList()
      val result = CompletableDeferred<CompilationResult>()
      val compilationIndicator = ProgressWrapper.wrap(indicator)


      // When the coroutine completes, make sure we also stop or cancel the indicator
      // depending on what happened with the co-routine.
      coroutineContext.job.invokeOnCompletion {
        try {
          if (compilationIndicator.isRunning) {
            if (coroutineContext.job.isCancelled) compilationIndicator.cancel() else compilationIndicator.stop()
          }
        }
        catch (t: Throwable) {
          // stop might throw if the indicator is already stopped
          log.warn(t)
        }
      }

      try {
        compileKtFiles(inputs, module, outputDirectory = outputDirectory)
        result.complete(CompilationResult.Success)
      }
      catch (t: CancellationException) {
        result.complete(CompilationResult.CompilationAborted())
      }
      catch (t: ProcessCanceledException) {
        result.complete(CompilationResult.CompilationAborted())
      }
      catch (t: Throwable) {
        when {
          t.isCompilationError() || t.cause.isCompilationError() -> result.complete(CompilationResult.CompilationError(t))
          !isKotlinPluginBundled() -> {
            // The embedded Compiler is only guaranteed to work with the bundled plugin. If the user has changed the plugin,
            // we can not guarantee Fast Preview to work.
            result.complete(CompilationResult.RequestException(NotUsingKotlinBundledPlugin(t)))
          }

          else -> result.complete(CompilationResult.RequestException(t))
        }
      }

      result.await()
    }
  }

  override fun dispose() {}
}