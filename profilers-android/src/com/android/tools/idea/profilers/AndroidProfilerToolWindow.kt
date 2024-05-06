/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.profilers

import com.android.ddmlib.IDevice
import com.android.tools.adtui.model.AspectObserver
import com.android.tools.idea.model.StudioAndroidModuleInfo
import com.android.tools.idea.transport.TransportService
import com.android.tools.idea.transport.TransportServiceProxy.Companion.getDeviceManufacturer
import com.android.tools.idea.transport.TransportServiceProxy.Companion.getDeviceModel
import com.android.tools.nativeSymbolizer.ProjectSymbolSource
import com.android.tools.nativeSymbolizer.SymbolFilesLocator
import com.android.tools.nativeSymbolizer.SymbolSource
import com.android.tools.profiler.proto.Common
import com.android.tools.profilers.IdeProfilerComponents
import com.android.tools.profilers.Notification
import com.android.tools.profilers.ProfilerAspect
import com.android.tools.profilers.ProfilerClient
import com.android.tools.profilers.StudioProfilers
import com.android.tools.profilers.taskbased.common.constants.TaskBasedUxIcons
import com.android.tools.profilers.tasks.ProfilerTaskTabs
import com.android.tools.profilers.tasks.ProfilerTaskType
import com.android.tools.profilers.tasks.args.TaskArgs
import com.android.tools.profilers.tasks.taskhandlers.ProfilerTaskHandler
import com.android.tools.profilers.tasks.taskhandlers.ProfilerTaskHandlerFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import org.jetbrains.annotations.VisibleForTesting
import java.awt.BorderLayout
import java.io.File
import java.util.function.Supplier
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

class AndroidProfilerToolWindow(private val window: ToolWindowWrapper, private val project: Project) : AspectObserver(), Disposable {
  private val ideProfilerServices: IntellijProfilerServices
  private val ideProfilerComponents: IdeProfilerComponents
  val profilers: StudioProfilers
  private var currentTaskHandler: ProfilerTaskHandler? = null
  private val taskHandlers = HashMap<ProfilerTaskType, ProfilerTaskHandler>()

  private lateinit var homeTab: StudioProfilersHomeTab
  private lateinit var homePanel: JPanel
  private lateinit var pastRecordingsTab: StudioProfilersPastRecordingsTab
  private lateinit var pastRecordingsPanel: JPanel
  private lateinit var profilersTab: StudioProfilersTab
  lateinit var profilersPanel: JPanel
    private set

  init {
    val symbolSource: SymbolSource = ProjectSymbolSource(project)
    val symbolLocator = SymbolFilesLocator(symbolSource)
    ideProfilerServices = IntellijProfilerServices(project, symbolLocator)
    Disposer.register(this, ideProfilerServices)

    // Ensures the transport service is initialized.
    TransportService.getInstance()

    val client = ProfilerClient(TransportService.channelName)
    profilers = StudioProfilers(client, ideProfilerServices, taskHandlers,
                                { taskType, args -> ProfilerTaskTabs.create(project, taskType, args) }, { ProfilerTaskTabs.open(project) })

    val navigator = ideProfilerServices.codeNavigator
    // CPU ABI architecture, when needed by the code navigator, should be retrieved from StudioProfiler selected session.
    navigator.cpuArchSource = Supplier { profilers.sessionsManager.selectedSessionMetaData.processAbi }

    profilers.addDependency(this).onChange(ProfilerAspect.STAGE) { stageChanged() }

    // Attempt to find the last-run process and start profiling it. This covers the case where the user presses "Run" (without profiling),
    // but then opens the profiling window manually.
    val processInfo = project.getUserData(LAST_RUN_APP_INFO)
    if (processInfo != null) {
      profilers.setPreferredProcess(processInfo.deviceName,
                                    processInfo.processName) { p: Common.Process? -> processInfo.processFilter.invoke(p!!) }
      project.putUserData(LAST_RUN_APP_INFO, null)
    }
    else {
      StartupManager.getInstance(project).runWhenProjectIsInitialized { profilers.preferredProcessName = getPreferredProcessName(project) }
    }

    ideProfilerComponents = IntellijProfilerComponents(project, ideProfilerServices.featureTracker)

    // Create and store the task handlers in a map.
    initializeTaskHandlers()

    if (ideProfilerServices.featureConfig.isTaskBasedUxEnabled) {
      // Initialize the two static/un-closable tabs: home and past recordings tabs.
      homeTab = StudioProfilersHomeTab(profilers, ideProfilerComponents)
      homePanel = JPanel(BorderLayout())
      homePanel.removeAll()
      homePanel.add(homeTab.view.panel)
      homePanel.revalidate()
      homePanel.repaint()
      pastRecordingsTab = StudioProfilersPastRecordingsTab(profilers, ideProfilerComponents)
      pastRecordingsPanel = JPanel(BorderLayout())
      pastRecordingsPanel.removeAll()
      pastRecordingsPanel.add(pastRecordingsTab.view.panel)
      pastRecordingsPanel.revalidate()
      pastRecordingsPanel.repaint()
    }
    // The Profiler tab is initialized here with the home tab so that the view bindings will be ready in the case the user imports a file
    // from a fresh/un-opened Profiler tool window state. While entering a stage from an uninitialized Profiler state after importing is
    // not a possible flow in the Task-Based UX, the initialization of the Profiler tab logic is used for both the Sessions-based Profiler
    // tab and the Task-Based UX Profiler tab, so it must be called in a place that accommodates both tabs.
    initializeProfilerTab()
  }

  private fun initializeTaskHandlers() {
    taskHandlers.clear()
    taskHandlers.putAll(ProfilerTaskHandlerFactory.createTaskHandlers(profilers.sessionsManager))
  }

  @VisibleForTesting
  fun createNewTab(component: JComponent, tabName: String, isCloseable: Boolean, icon: Icon? = null) {
    val contentManager = window.getContentManager()
    val content = contentManager.factory.createContent(component, tabName, false).also { content ->
      content.isCloseable = isCloseable
      icon?.let {
        content.icon = it
        content.putUserData(ToolWindow.SHOW_CONTENT_ICON, true)
      }
    }
    contentManager.addContent(content)
    contentManager.setSelectedContent(content)
  }

  private fun findHomeTab(): Content? {
    val contentManager = window.getContentManager()
    return if (contentManager.contentCount == 0) null else contentManager.getContent(0)
  }

  private fun findPastRecordingsTab(): Content? {
    val contentManager = window.getContentManager()
    return if (contentManager.contentCount <= 1) null else contentManager.getContent(1)
  }

  private fun findTaskTab(): Content? {
    val contentManager = window.getContentManager()
    return when (contentManager.contentCount) {
      0 -> null
      1 -> null
      2 -> null
      3 -> contentManager.getContent(2)
      else -> throw RuntimeException("Profiler window has more than 3 tabs")
    }
  }

  private fun initializeProfilerTab() {
    profilersTab = if (ideProfilerServices.featureConfig.isTaskBasedUxEnabled) StudioProfilersTaskTab(profilers, ideProfilerComponents)
    else StudioProfilersSessionTab(profilers, window, ideProfilerComponents, project)
    Disposer.register(this, profilersTab)

    profilersPanel = JPanel(BorderLayout())
    profilersPanel.removeAll()
    profilersPanel.add(profilersTab.view.component)
    profilersPanel.revalidate()
    profilersPanel.repaint()
  }

  fun openHomeTab() {
    val homeTab = findHomeTab()
    if (homeTab != null) {
      window.getContentManager().setSelectedContent(homeTab)
    }
    else {
      createNewTab(homePanel, PROFILER_HOME_TAB_NAME, false)
    }
  }

  fun openPastRecordingsTab() {
    val pastRecordingsTab = findPastRecordingsTab()
    if (pastRecordingsTab != null) {
      window.getContentManager().setSelectedContent(pastRecordingsTab)
    }
    else {
      createNewTab(pastRecordingsPanel, PROFILER_PAST_RECORDINGS_TAB_NAME, false)
    }
  }

  /**
   * Creates and opens a Profiler task tab for a specified task type. If a task tab has been opened beforehand, the existing tab is reused.
   */
  fun createTaskTab(taskType: ProfilerTaskType, taskArgs: TaskArgs?) {
    val taskTab = findTaskTab()
    val taskName = taskHandlers[taskType]?.getTaskName() ?: "Task Not Supported Yet"
    val taskIcon = TaskBasedUxIcons.getTaskIcon(taskType).swingIcon
    if (taskTab != null) {
      taskTab.displayName = taskName
      window.getContentManager().setSelectedContent(taskTab)
      window.getContentManager().selectedContent!!.icon = taskIcon
    }
    else {
      createNewTab(profilersPanel, taskName, true, taskIcon)
    }
    currentTaskHandler?.exit()
    currentTaskHandler = taskHandlers[taskType]
    currentTaskHandler?.let { taskHandler ->
      val enterSuccessful = taskHandler.enter(taskArgs)
    }
  }

  /**
   * Opens an existing Profiler task tab. There is at most one existing task tab at any time that can be opened.
   */
  fun openTaskTab() {
    val taskTab = findTaskTab()
    if (taskTab != null) {
      window.getContentManager().setSelectedContent(taskTab)
    }
  }

  /** Sets the profiler's auto-profiling process in case it has been unset.  */
  fun profile(processInfo: PreferredProcessInfo) {
    profilers.setPreferredProcess(processInfo.deviceName, processInfo.processName) { p: Common.Process? ->
      processInfo.processFilter.invoke(p!!)
    }
  }

  /**
   * Disables auto device+process selection.
   * See: [StudioProfilers.setAutoProfilingEnabled]
   */
  fun disableAutoProfiling() {
    profilers.autoProfilingEnabled = false
  }

  /**
   * Tries to import a file into an imported session of the profilers and shows an error balloon if it fails to do so.
   */
  fun openFile(file: VirtualFile) {
    if (!profilers.sessionsManager.importSessionFromFile(File(file.path))) {
      ideProfilerServices.showNotification(OPEN_FILE_FAILURE_NOTIFICATION)
    }
  }

  override fun dispose() {
    profilers.stop()
  }

  private fun stageChanged() {
    if (profilers.isStopped) {
      window.removeContent()
    }
  }

  companion object {

    private const val PROFILER_HOME_TAB_NAME = "Home"
    private const val PROFILER_PAST_RECORDINGS_TAB_NAME = "Past Recordings"

    /**
     * Key for storing the last app that was run when the profiler window was not opened. This allows the Profilers to start auto-profiling
     * that app when the user opens the window at a later time.
     */
    @JvmField
    val LAST_RUN_APP_INFO = Key.create<PreferredProcessInfo>("Profiler.Last.Run.App")
    private val OPEN_FILE_FAILURE_NOTIFICATION = Notification(
      Notification.Severity.ERROR,
      "Failed to open file",
      "The profiler was unable to open the selected file. Please try opening it " +
      "again or select a different file.",
      null)

    /**
     * Analogous to [StudioProfilers.buildDeviceName] but works with an [IDevice] instead.
     *
     * @return A string of the format: {Manufacturer Model}.
     */
    @JvmStatic
    fun getDeviceDisplayName(device: IDevice): String {
      val manufacturer = getDeviceManufacturer(device)
      val model = getDeviceModel(device)
      val serial = device.serialNumber
      return getDeviceDisplayName(manufacturer, model, serial)
    }

    /**
     * Gets the display name of a device with the given manufacturer, model, and serial string.
     */
    fun getDeviceDisplayName(manufacturer: String, model: String, serial: String): String {
      var deviceModel = model
      val deviceNameBuilder = StringBuilder()
      val suffix = String.format("-%s", serial)
      if (deviceModel.endsWith(suffix)) {
        deviceModel = deviceModel.substring(0, deviceModel.length - suffix.length)
      }
      if (!StringUtil.isEmpty(manufacturer) && !deviceModel.uppercase().startsWith(manufacturer.uppercase())) {
        deviceNameBuilder.append(manufacturer)
        deviceNameBuilder.append(" ")
      }
      deviceNameBuilder.append(deviceModel)

      return deviceNameBuilder.toString()
    }

    private fun getPreferredProcessName(project: Project): String? {
      for (module in ModuleManager.getInstance(project).modules) {
        val moduleName = getModuleName(module)
        if (moduleName != null) {
          return moduleName
        }
      }
      return null
    }

    private fun getModuleName(module: Module): String? {
      val moduleInfo = StudioAndroidModuleInfo.getInstance(module)
      if (moduleInfo != null) {
        val pkg = moduleInfo.packageName
        if (pkg != null) {
          return pkg
        }
      }
      return null
    }
  }
}