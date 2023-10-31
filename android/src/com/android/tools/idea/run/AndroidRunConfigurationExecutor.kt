/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.run

import com.android.AndroidProjectTypes
import com.android.ddmlib.IDevice
import com.android.tools.deployer.DeployerException
import com.android.tools.deployer.model.App
import com.android.tools.idea.concurrency.AndroidDispatchers.uiThread
import com.android.tools.idea.deploy.DeploymentConfiguration
import com.android.tools.idea.editors.literals.LiveEditService
import com.android.tools.idea.execution.common.AndroidConfigurationExecutor
import com.android.tools.idea.execution.common.AndroidExecutionException
import com.android.tools.idea.execution.common.ApplicationDeployer
import com.android.tools.idea.execution.common.ApplicationTerminator
import com.android.tools.idea.execution.common.DeployOptions
import com.android.tools.idea.execution.common.RunConfigurationNotifier
import com.android.tools.idea.execution.common.adb.shell.tasks.launchSandboxSdk
import com.android.tools.idea.execution.common.clearAppStorage
import com.android.tools.idea.execution.common.debug.AndroidDebuggerState
import com.android.tools.idea.execution.common.debug.DebugSessionStarter
import com.android.tools.idea.execution.common.deploy.deployAndHandleError
import com.android.tools.idea.execution.common.getProcessHandlersForDevices
import com.android.tools.idea.execution.common.processhandler.AndroidProcessHandler
import com.android.tools.idea.execution.common.shouldDebugSandboxSdk
import com.android.tools.idea.execution.common.stats.RunStats
import com.android.tools.idea.execution.common.stats.track
import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.run.ShowLogcatListener.Companion.getShowLogcatLinkText
import com.android.tools.idea.run.activity.launch.DeepLinkLaunch
import com.android.tools.idea.run.configuration.execution.ApplicationDeployerImpl
import com.android.tools.idea.run.configuration.execution.createRunContentDescriptor
import com.android.tools.idea.run.configuration.execution.getDevices
import com.android.tools.idea.run.configuration.execution.println
import com.android.tools.idea.run.configuration.isDebug
import com.android.tools.idea.run.tasks.RunInstantApp
import com.android.tools.idea.run.util.LaunchUtils
import com.android.tools.idea.util.androidFacet
import com.intellij.execution.ExecutionException
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.util.Disposer
import com.intellij.xdebugger.impl.XDebugSessionImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AndroidRunConfigurationExecutor(
  private val applicationIdProvider: ApplicationIdProvider,
  private val env: ExecutionEnvironment,
  val deviceFutures: DeviceFutures,
  private val apkProvider: ApkProvider,
  private val liveEditService: LiveEditService = LiveEditService.getInstance(env.project),
  private val applicationDeployer: ApplicationDeployer = ApplicationDeployerImpl(env.project, RunStats.from(env))
) : AndroidConfigurationExecutor {
  val project = env.project
  override val configuration = env.runProfile as AndroidRunConfiguration
  private val settings = env.runnerAndConfigurationSettings as RunnerAndConfigurationSettings

  val facet = configuration.configurationModule.module?.androidFacet ?: throw RuntimeException("Cannot get AndroidFacet")

  private val LOG = Logger.getInstance(this::class.java)

  override fun run(indicator: ProgressIndicator): RunContentDescriptor = runBlockingCancellable {
    val (packageName, devices) = getApplicationIdAndDevices(indicator)

    settings.getProcessHandlersForDevices(project, devices).forEach { it.destroyProcess() }

    waitPreviousProcessTermination(devices, packageName, indicator)

    val processHandler = AndroidProcessHandler(packageName, { it.forceStop(packageName) })
    val console = createConsole()

    fillStats(RunStats.from(env), packageName)

    devices.forEach {
      if (configuration.CLEAR_LOGCAT) {
        project.messageBus.syncPublisher(ClearLogcatListener.TOPIC).clearLogcat(it.serialNumber)
      }
      if (configuration.CLEAR_APP_STORAGE) {
        clearAppStorage(project, it, packageName, RunStats.from(env))
      }
      LaunchUtils.initiateDismissKeyguard(it)
    }

    console.printLaunchTaskStartedMessage("Launching")

    if (shouldDeployAsInstant()) {
      deployAsInstantApp(devices, console)
    } else {
      indicator.text = "Launching on devices"
      devices.map { device ->
        async {
          LOG.info("Launching on device ${device.name}")

          //Deploy
          if (configuration.DEPLOY) {
            val apks = apkInfosSafe(device)
            val deployResults =
              deployAndHandleError(env, { apks.map { applicationDeployer.fullDeploy(device, it, configuration.deployOptions, indicator) } })
            notifyLiveEditService(device, packageName)

            if (shouldDebugSandboxSdk(apkProvider, device, configuration.androidDebuggerContext.getAndroidDebuggerState()!!)) {
              launchSandboxSdk(device, packageName)
            }

            val mainApp = deployResults.find { it.app.appId == packageName }
              ?: throw RuntimeException("No app installed matching packageName provided by ApplicationIdProvider")
            launch(mainApp.app, device, console, isDebug = false)
          }
        }
      }.awaitAll()
    }

    devices.forEach { device ->
      processHandler.addTargetDevice(device)
      if (!StudioFlags.RUNDEBUG_LOGCAT_CONSOLE_OUTPUT_ENABLED.get()) {
        console.printHyperlink(getShowLogcatLinkText(device)) { project ->
          project.messageBus.syncPublisher(ShowLogcatListener.TOPIC).showLogcat(device, packageName)
        }
      }
      if (configuration.SHOW_LOGCAT_AUTOMATICALLY) {
        project.messageBus.syncPublisher(ShowLogcatListener.TOPIC).showLogcat(device, packageName)
      }
    }

    createRunContentDescriptor(processHandler, console, env)
  }

  private fun deployAsInstantApp(devices: List<IDevice>, console: ConsoleView) {
    val state: DeepLinkLaunch.State = configuration.getLaunchOptionState(AndroidRunConfiguration.LAUNCH_DEEP_LINK) as DeepLinkLaunch.State
    devices.forEach { device ->
      RunStats.from(env).track("RUN_INSTANT_APP") {
        RunInstantApp(apkInfosSafe(device), state.DEEP_LINK, configuration.disabledDynamicFeatures).run(console, device)
      }
    }
  }

  private fun shouldDeployAsInstant(): Boolean {
    return facet.configuration.projectType == AndroidProjectTypes.PROJECT_TYPE_INSTANTAPP || configuration.DEPLOY_AS_INSTANT
  }

  private fun notifyLiveEditService(device: IDevice, packageName: String) {
    try {
      AndroidLiveLiteralDeployMonitor.startMonitor(project, packageName, device)
      LiveEditHelper().invokeLiveEdit(liveEditService, env, applicationIdProvider, apkProvider, device)
    } catch (e: Exception) {

      // Monitoring should always start successfully.
      RunConfigurationNotifier.notifyWarning(project, configuration.name, "Error starting live edit.\n$e")
    }
  }

  private suspend fun waitPreviousProcessTermination(devices: List<IDevice>, applicationId: String, indicator: ProgressIndicator) =
    coroutineScope {
      indicator.text = "Terminating the app"
      val results = devices.map { async { ApplicationTerminator(it, applicationId).killApp() } }.awaitAll()
      if (results.any { !it }) {
        throw ExecutionException("Couldn't terminate previous instance of app")
      }
    }

  override fun debug(indicator: ProgressIndicator): RunContentDescriptor = runBlockingCancellable {
    val (packageName, devices) = getApplicationIdAndDevices(indicator)

    if (devices.size != 1) {
      throw ExecutionException("Cannot launch a debug session on more than 1 device.")
    }
    fillStats(RunStats.from(env), packageName)

    settings.getProcessHandlersForDevices(project, devices).forEach { it.destroyProcess() }

    RunStats.from(env).track("waitForProcessTermination") {
      waitPreviousProcessTermination(devices, packageName, indicator)
    }

    val console = createConsole()
    val device = devices.single()

    if (configuration.CLEAR_LOGCAT) {
      project.messageBus.syncPublisher(ClearLogcatListener.TOPIC).clearLogcat(device.serialNumber)
    }
    if (configuration.CLEAR_APP_STORAGE) {
      clearAppStorage(project, device, packageName, RunStats.from(env))
    }
    LaunchUtils.initiateDismissKeyguard(device)

    console.printLaunchTaskStartedMessage("Launching")

    if (shouldDeployAsInstant()) {
      deployAsInstantApp(devices, console)
    } else {
      indicator.text = "Launching on devices"
      LOG.info("Launching on device ${device.name}")

      //Deploy
      if (configuration.DEPLOY) {
        val apks = apkInfosSafe(device)
        val deployResults =
          deployAndHandleError(env, { apks.map { applicationDeployer.fullDeploy(device, it, configuration.deployOptions, indicator) } })

        notifyLiveEditService(device, packageName)

        if (shouldDebugSandboxSdk(apkProvider, device, configuration.androidDebuggerContext.getAndroidDebuggerState()!!)) {
          launchSandboxSdk(device, packageName)
        }

        val mainApp = deployResults.find { it.app.appId == packageName }
          ?: throw RuntimeException("No app installed matching packageName provided by ApplicationIdProvider")
        launch(mainApp.app, device, console, isDebug = true)
      }
    }

    indicator.text = "Connecting debugger"
    val session = startDebugSession(device, packageName, indicator, console)
    if (configuration.SHOW_LOGCAT_AUTOMATICALLY) {
      project.messageBus.syncPublisher(ShowLogcatListener.TOPIC).showLogcat(device, packageName)
    }
    session.runContentDescriptor
  }

  private suspend fun startDebugSession(
    device: IDevice, packageName: String, indicator: ProgressIndicator, console: ConsoleView
  ):XDebugSessionImpl  {
    val debugger = configuration.androidDebuggerContext.androidDebugger
      ?: throw ExecutionException("Unable to determine debugger to use for this launch")
    LOG.info("Using debugger: " + debugger.id)
    val debuggerState = configuration.androidDebuggerContext.getAndroidDebuggerState<AndroidDebuggerState>()
      ?: throw ExecutionException("Unable to determine androidDebuggerState to use for this launch")

    return DebugSessionStarter.attachDebuggerToStartedProcess(
      device,
      packageName,
      env,
      debugger,
      debuggerState,
      destroyRunningProcess = { d -> d.forceStop(packageName) },
      indicator,
      console,
      15
    )
  }

  override fun applyChanges(indicator: ProgressIndicator): RunContentDescriptor = runBlockingCancellable {
    val (packageName, devices) = getApplicationIdAndDevices(indicator)

    /**
     * We use [distinct] because there can be more than one RunContentDescriptor for given configuration and given devices.
     *
     * Every time user does AC or ACC we are obligated to create a new RunContentDescriptor. So we create, but don't show it in UI by setting [RunContentDescriptor.isHiddenContent] to false.
     * Multiple [RunContentDescriptor] -> multiple [ProcessHandler]. But it's the same instance of [ProcessHandler].
     */
    val processHandlers = settings.getProcessHandlersForDevices(project, devices).distinct()

    /**
     * Searching for first not hidden [RunContentDescriptor].
     */
    val existingRunContentDescriptor = processHandlers.mapNotNull {
      withContext(uiThread) {
        RunContentManager.getInstance(project).findContentDescriptor(env.executor, it)?.takeIf { !it.isHiddenContent }
      }
    }.firstOrNull()

    var needsNewRunContentDescriptor = existingRunContentDescriptor == null

    fillStats(RunStats.from(env), packageName)

    val console = existingRunContentDescriptor?.executionConsole as? ConsoleView ?: createConsole()
    console.printLaunchTaskStartedMessage("Applying changes to")

    // A list of devices that we have launched application successfully.
    indicator.text = "Launching on devices"
    devices.map { device ->
      async {
        LOG.info("Launching on device ${device.name}")

        //Deploy
        val apks = apkInfosSafe(device)
        val deployResults = deployAndHandleError(
          env,
          { apks.map { applicationDeployer.applyChangesDeploy(device, it, configuration.deployOptions, indicator) } },
          isApplyChangesFallbackToRun()
        )
        notifyLiveEditService(device, packageName)

        if (deployResults.any { it.needsRestart }) {
          val mainApp = deployResults.find { it.app.appId == packageName }
            ?: throw RuntimeException("No app installed matching packageName provided by ApplicationIdProvider")
          RunConfigurationNotifier.notifyInfo(project, configuration.name, "Swap failed, needs restart")
          waitPreviousProcessTermination(listOf(device), packageName, indicator)
          launch(mainApp.app, device, console, isDebug = false)
          needsNewRunContentDescriptor = true
        }
      }
    }.awaitAll()

    if (needsNewRunContentDescriptor || existingRunContentDescriptor?.processHandler == null || existingRunContentDescriptor.processHandler?.isProcessTerminated == true) {
      existingRunContentDescriptor?.processHandler?.detachProcess()
      val processHandler = AndroidProcessHandler(packageName).apply { devices.forEach { addTargetDevice(it) } }
      withContext(uiThread) { createRunContentDescriptor(processHandler, createConsole(), env) }
    } else {
      HiddenRunContentDescriptor(existingRunContentDescriptor)
    }
  }

  private fun createConsole(): ConsoleView {
    val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
    Disposer.register(project, console)
    return console
  }

  override fun applyCodeChanges(indicator: ProgressIndicator) = runBlockingCancellable {
    val (packageName, devices) = getApplicationIdAndDevices(indicator)

    /**
     * We use [distinct] because there can be more than one RunContentDescriptor for given configuration and given devices.
     *
     * Every time user does AC or ACC we are obligated to create a new RunContentDescriptor. So we create, but don't show it in UI by setting [RunContentDescriptor.isHiddenContent] to false.
     * Multiple [RunContentDescriptor] -> multiple [ProcessHandler]. But it's the same instance of [ProcessHandler].
     */
    val processHandlers = settings.getProcessHandlersForDevices(project, devices).distinct()

    /**
     * Searching for first not hidden [RunContentDescriptor].
     */
    val existingRunContentDescriptor = processHandlers.mapNotNull {
      withContext(uiThread) {
        RunContentManager.getInstance(project).findContentDescriptor(env.executor, it)?.takeIf { !it.isHiddenContent }
      }
    }.firstOrNull()

    var needsNewRunContentDescriptor = existingRunContentDescriptor == null

    fillStats(RunStats.from(env), packageName)

    val console = existingRunContentDescriptor?.executionConsole as? ConsoleView ?: createConsole()
    console.printLaunchTaskStartedMessage("Applying code changes to")

    devices.map { device ->
      async {
        LOG.info("Launching on device ${device.name}")
        val apks = apkInfosSafe(device)
        val deployResults = deployAndHandleError(
          env,
          { apks.map { applicationDeployer.applyCodeChangesDeploy(device, it, configuration.deployOptions, indicator) } },
          isApplyCodeChangesFallbackToRun()
        )

        notifyLiveEditService(device, packageName)

        if (deployResults.any { it.needsRestart }) {
          val mainApp = deployResults.find { it.app.appId == packageName }
            ?: throw RuntimeException("No app installed matching packageName provided by ApplicationIdProvider")

          RunConfigurationNotifier.notifyInfo(project, configuration.name, "Swap failed, needs restart")
          waitPreviousProcessTermination(listOf(device), packageName, indicator)
          launch(mainApp.app, device, console, isDebug = env.executor.isDebug)
          needsNewRunContentDescriptor = true
        }
      }
    }.awaitAll()

    if (needsNewRunContentDescriptor ||
      existingRunContentDescriptor?.processHandler == null ||
      existingRunContentDescriptor.processHandler?.isProcessTerminated == true
    ) {
      existingRunContentDescriptor?.processHandler?.detachProcess()
      if (env.executor.isDebug) {
        startDebugSession(devices.single(), packageName, indicator, createConsole()).runContentDescriptor
      } else {
        val processHandler = AndroidProcessHandler(packageName).apply { devices.forEach { addTargetDevice(it) } }
        withContext(uiThread) { createRunContentDescriptor(processHandler, createConsole(), env) }
      }
    } else {
      HiddenRunContentDescriptor(existingRunContentDescriptor)
    }
  }

  inner class HiddenRunContentDescriptor(existingRunContentDescriptor: RunContentDescriptor) : RunContentDescriptor(
    existingRunContentDescriptor.executionConsole,
    existingRunContentDescriptor.processHandler,
    existingRunContentDescriptor.component,
    existingRunContentDescriptor.displayName
  ) {
    override fun isHiddenContent() = true

    init {
      Disposer.register(project, this)
    }
  }

  @Throws(ExecutionException::class)
  private fun apkInfosSafe(device: IDevice): MutableCollection<ApkInfo> = try {
    apkProvider.getApks(device)
  } catch (e: ApkProvisionException) {
    throw ExecutionException(e)
  }

  @Throws(ExecutionException::class)
  private suspend fun getApplicationIdAndDevices(indicator: ProgressIndicator): Pair<String, List<IDevice>> {
    val packageName = try {
      applicationIdProvider.packageName
    } catch (e: ApkProvisionException) {
      throw ExecutionException(e)
    }
    val devices = getDevices(deviceFutures, indicator, RunStats.from(env))
    return Pair(packageName, devices)
  }

  private fun fillStats(stats: RunStats, packageName: String) {
    stats.setPackage(packageName)
    stats.setApplyChangesFallbackToRun(isApplyChangesFallbackToRun())
    stats.setApplyCodeChangesFallbackToRun(isApplyCodeChangesFallbackToRun())
    stats.setRunAlwaysInstallWithPm(configuration.ALWAYS_INSTALL_WITH_PM)
    stats.setIsComposeProject(LiveEditService.usesCompose(project))
  }

  private fun isApplyCodeChangesFallbackToRun(): Boolean {
    return DeploymentConfiguration.getInstance().APPLY_CODE_CHANGES_FALLBACK_TO_RUN
  }

  private fun isApplyChangesFallbackToRun(): Boolean {
    return DeploymentConfiguration.getInstance().APPLY_CHANGES_FALLBACK_TO_RUN
  }

  private fun ConsoleView.printLaunchTaskStartedMessage(launchVerb: String) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
    println("$dateFormat: $launchVerb ${configuration.name} on '${env.executionTarget.displayName}'.")
  }

  @Throws(ExecutionException::class)
  fun launch(app: App, device: IDevice, consoleView: ConsoleView, isDebug: Boolean) {
    val amStartOptions = StringBuilder()

    for (taskContributor in AndroidLaunchTaskContributor.EP_NAME.extensionList) {
      val amOptions = taskContributor.getAmStartOptions(app.appId, configuration, device, env.executor)
      amStartOptions.append(if (amStartOptions.isEmpty()) "" else " ").append(amOptions)
    }
    project.messageBus.syncPublisher(DeviceHeadsUpListener.TOPIC).launchingApp(device.serialNumber, project)
    try {
      configuration.launch(app, device, facet, amStartOptions.toString(), isDebug, apkProvider, consoleView, RunStats.from(env))
    } catch (e: DeployerException) {
      throw AndroidExecutionException(e.id, e.message)
    }
  }
}

private val AndroidRunConfiguration.deployOptions
  get() = DeployOptions(disabledDynamicFeatures, PM_INSTALL_OPTIONS, ALL_USERS, ALWAYS_INSTALL_WITH_PM)
