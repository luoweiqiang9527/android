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
package com.android.tools.idea.layoutinspector.runningdevices

import com.android.annotations.concurrency.UiThread
import com.android.tools.idea.flags.ExperimentalSettingsConfigurable
import com.android.tools.idea.layoutinspector.LayoutInspector
import com.android.tools.idea.layoutinspector.LayoutInspectorBundle
import com.android.tools.idea.layoutinspector.LayoutInspectorProjectService
import com.android.tools.idea.layoutinspector.model.StatusNotificationAction
import com.android.tools.idea.layoutinspector.runningdevices.ui.SelectedTabState
import com.android.tools.idea.layoutinspector.runningdevices.ui.TabComponents
import com.android.tools.idea.layoutinspector.settings.LayoutInspectorSettings
import com.android.tools.idea.streaming.RUNNING_DEVICES_TOOL_WINDOW_ID
import com.android.tools.idea.streaming.core.AbstractDisplayView
import com.android.tools.idea.streaming.core.DEVICE_ID_KEY
import com.android.tools.idea.streaming.core.DISPLAY_VIEW_KEY
import com.android.tools.idea.streaming.core.DeviceId
import com.android.tools.idea.streaming.core.STREAMING_CONTENT_PANEL_KEY
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.ui.EditorNotificationPanel.Status
import com.intellij.ui.content.Content
import com.intellij.ui.scale.JBUIScale
import javax.swing.JComponent

const val SHOW_EXPERIMENTAL_WARNING_KEY =
  "com.android.tools.idea.layoutinspector.runningdevices.experimental.notification.show"
const val EMBEDDED_EXPERIMENTAL_MESSAGE_KEY = "embedded.inspector.experimental.notification.message"

const val SPLITTER_KEY =
  "com.android.tools.idea.layoutinspector.runningdevices.LayoutInspectorManager.Splitter"

private const val DEFAULT_WINDOW_WIDTH = 800

/**
 * Object used to track tabs that have Layout Inspector enabled across multiple projects. Layout
 * Inspector should be enabled only once for each tab, across projects. Multiple projects connecting
 * to the same process is not a supported use case by Layout Inspector.
 */
object LayoutInspectorManagerGlobalState {
  val tabsWithLayoutInspector = mutableSetOf<DeviceId>()
}

/** Responsible for managing Layout Inspector in Running Devices Tool Window. */
interface LayoutInspectorManager : Disposable {
  companion object {
    @JvmStatic
    fun getInstance(project: Project): LayoutInspectorManager {
      return project.getService(LayoutInspectorManager::class.java)
    }
  }

  fun interface StateListener {
    /**
     * Called each time the state of [LayoutInspectorManager] changes. Which happens each time
     * Layout Inspector is enabled or disabled for a tab.
     */
    fun onStateUpdate(state: Set<DeviceId>)
  }

  fun addStateListener(listener: StateListener)

  /** Injects or removes Layout Inspector in the tab associated to [deviceId]. */
  fun enableLayoutInspector(deviceId: DeviceId, enable: Boolean)

  /** Returns true if Layout Inspector is enabled for [deviceId], false otherwise. */
  fun isEnabled(deviceId: DeviceId): Boolean
}

/** This class is meant to be used on the UI thread, to avoid concurrency issues. */
@UiThread
private class LayoutInspectorManagerImpl(private val project: Project) : LayoutInspectorManager {

  /** Tabs on which Layout Inspector is enabled. */
  private var tabsWithLayoutInspector = setOf<DeviceId>()
    set(value) {
      ThreadingAssertions.assertEventDispatchThread()
      if (value == field) {
        return
      }

      val tabsAdded = value - field
      val tabsRemoved = field - value

      // check if the selected tab was removed
      if (tabsRemoved.contains(selectedTab?.deviceId)) {
        selectedTab = null
      }

      field = value

      LayoutInspectorManagerGlobalState.tabsWithLayoutInspector.addAll(tabsAdded)
      LayoutInspectorManagerGlobalState.tabsWithLayoutInspector.removeAll(tabsRemoved)

      updateListeners()
    }

  /** The tab on which Layout Inspector is running */
  private var selectedTab: SelectedTabState? = null
    set(value) {
      ThreadingAssertions.assertEventDispatchThread()
      if (field == value) {
        return
      }

      val previousTab = field
      if (previousTab != null) {
        // Dispose to trigger clean up.
        Disposer.dispose(previousTab.tabComponents)
        previousTab.layoutInspector.stopInspector()
        previousTab.layoutInspector.deviceModel?.forcedDeviceSerialNumber = null
        // Calling foregroundProcessDetection.start and stop from LayoutInspectorManager is a
        // workaround used to prevent foreground process detection from running in the background
        // even when embedded LI is not enabled on any device. This won't be necessary when we will
        // be able to create a new instance of LayoutInspector for each tab in Running Devices,
        // instead of having a single global instance of LayoutInspector shared by all the tabs. See
        // b/304540563
        previousTab.layoutInspector.foregroundProcessDetection?.stop()
      }

      field = value

      if (value == null) {
        return
      }

      // lock device model to only allow connections to this device
      value.layoutInspector.deviceModel?.forcedDeviceSerialNumber = value.deviceId.serialNumber
      value.layoutInspector.foregroundProcessDetection?.start()

      val selectedDevice =
        value.layoutInspector.deviceModel?.devices?.find {
          it.serial == value.deviceId.serialNumber
        }
      // the device might not be available yet in app inspection
      if (selectedDevice != null) {
        // start polling
        value.layoutInspector.foregroundProcessDetection?.startPollingDevice(
          selectedDevice,
          // only stop polling if the previous tab is still open.
          previousTab?.deviceId in existingRunningDevicesTabs
        )
      }

      // inject Layout Inspector UI
      value.enableLayoutInspector()

      showExperimentalWarning(value.layoutInspector)
    }

  private val stateListeners = mutableListOf<LayoutInspectorManager.StateListener>()

  /**
   * The list of tabs currently open in Running Devices, with or without Layout Inspector enabled.
   */
  private var existingRunningDevicesTabs: List<DeviceId> = emptyList()

  init {
    check(LayoutInspectorSettings.getInstance().embeddedLayoutInspectorEnabled) {
      "LayoutInspectorManager is intended for use only in embedded Layout Inspector."
    }

    RunningDevicesStateObserver.getInstance(project)
      .addListener(
        object : RunningDevicesStateObserver.Listener {
          override fun onSelectedTabChanged(deviceId: DeviceId?) {
            selectedTab =
              if (deviceId != null && tabsWithLayoutInspector.contains(deviceId)) {
                // Layout Inspector was enabled for this tab.
                createTabState(deviceId)
              } else {
                // Layout Inspector was not enabled for this tab.
                null
              }
          }

          override fun onExistingTabsChanged(existingTabs: List<DeviceId>) {
            existingRunningDevicesTabs = existingTabs
            // If the Running Devices Tool Window is collapsed, all tabs are removed.
            // We don't want to update our state when this happens, because it means we would lose
            // track of which tabs had Layout Inspector.
            // So instead we keep the tab state forever.
            // So if an emulator is disconnected with Layout Inspector turned on and later
            // restarted, Layout Inspector will be on again.
          }

          override fun onToolWindowHidden() {
            clearSelectedTab()
          }

          override fun onToolWindowShown(selectedDeviceId: DeviceId?) {
            restoreSelectedTab(selectedDeviceId)
          }
        }
      )
  }

  private var shouldRestoreToolWindowState: Boolean = false

  /** Restore the state of the selected tab if it was manually cleared */
  private fun restoreSelectedTab(selectedDeviceId: DeviceId?) {
    if (!shouldRestoreToolWindowState) {
      return
    }

    shouldRestoreToolWindowState = false
    if (selectedDeviceId != null) {
      selectedTab = createTabState(selectedDeviceId)
    }
  }

  /** Clear the selected tab */
  private fun clearSelectedTab() {
    if (selectedTab == null) {
      return
    }

    shouldRestoreToolWindowState = true
    selectedTab = null
  }

  private fun createTabState(deviceId: DeviceId): SelectedTabState {
    ThreadingAssertions.assertEventDispatchThread()
    val runningDevicesContentManager = project.getRunningDevicesContentManager()
    val selectedTabContent =
      runningDevicesContentManager?.contents?.find { it.deviceId == deviceId }
    val selectedTabDataProvider = selectedTabContent?.component as? DataProvider

    val streamingContentPanel =
      selectedTabDataProvider?.getData(STREAMING_CONTENT_PANEL_KEY.name) as? JComponent
    val displayView =
      selectedTabDataProvider?.getData(DISPLAY_VIEW_KEY.name) as? AbstractDisplayView

    checkNotNull(selectedTabContent)
    checkNotNull(streamingContentPanel)
    checkNotNull(displayView)

    val tabComponents =
      TabComponents(
        disposable = selectedTabContent,
        tabContentPanel = streamingContentPanel,
        tabContentPanelContainer = streamingContentPanel.parent,
        displayView = displayView
      )

    return SelectedTabState(project, deviceId, tabComponents, project.getLayoutInspector())
  }

  override fun addStateListener(listener: LayoutInspectorManager.StateListener) {
    ThreadingAssertions.assertEventDispatchThread()
    updateListeners(listOf(listener))
    stateListeners.add(listener)
  }

  override fun enableLayoutInspector(deviceId: DeviceId, enable: Boolean) {
    ThreadingAssertions.assertEventDispatchThread()

    val toolWindow =
      ToolWindowManager.getInstance(project).getToolWindow(RUNNING_DEVICES_TOOL_WINDOW_ID)
        as? ToolWindowEx
    toolWindow?.let {
      val width = it.component.width
      // Resize the tool window width, to be equal to DEFAULT_WINDOW_WIDTH
      // stretchWidth resizes relatively to the current width of the tool window.
      it.stretchWidth(JBUIScale.scale(DEFAULT_WINDOW_WIDTH) - width)
    }
    if (enable) {
      if (tabsWithLayoutInspector.contains(deviceId)) {
        // do nothing if Layout Inspector is already enabled
        return
      }

      tabsWithLayoutInspector = tabsWithLayoutInspector + deviceId
      selectedTab = createTabState(deviceId)
    } else {
      if (!tabsWithLayoutInspector.contains(deviceId)) {
        // do nothing if Layout Inspector is not enabled
        return
      }

      tabsWithLayoutInspector = tabsWithLayoutInspector - deviceId
      if (selectedTab?.deviceId == deviceId) {
        selectedTab = null
      }
    }
  }

  override fun isEnabled(deviceId: DeviceId): Boolean {
    ThreadingAssertions.assertEventDispatchThread()
    return tabsWithLayoutInspector.contains(deviceId)
  }

  override fun dispose() {
    selectedTab = null
    tabsWithLayoutInspector = emptySet()
  }

  private fun updateListeners(
    listenersToUpdate: List<LayoutInspectorManager.StateListener> = stateListeners
  ) {
    ThreadingAssertions.assertEventDispatchThread()
    listenersToUpdate.forEach { listener -> listener.onStateUpdate(tabsWithLayoutInspector) }
  }

  private fun showExperimentalWarning(layoutInspector: LayoutInspector) {
    val notificationModel = layoutInspector.notificationModel
    val defaultValue = true
    val shouldShowWarning = {
      PropertiesComponent.getInstance().getBoolean(SHOW_EXPERIMENTAL_WARNING_KEY, defaultValue)
    }
    val setValue: (Boolean) -> Unit = {
      PropertiesComponent.getInstance().setValue(SHOW_EXPERIMENTAL_WARNING_KEY, it, defaultValue)
    }

    if (shouldShowWarning()) {
      notificationModel.addNotification(
        id = EMBEDDED_EXPERIMENTAL_MESSAGE_KEY,
        text = LayoutInspectorBundle.message(EMBEDDED_EXPERIMENTAL_MESSAGE_KEY),
        status = Status.Info,
        sticky = true,
        actions =
          listOf(
            StatusNotificationAction(LayoutInspectorBundle.message("do.not.show.again")) {
              notification ->
              setValue(false)
              notificationModel.removeNotification(notification.id)
            },
            StatusNotificationAction(LayoutInspectorBundle.message("opt.out")) {
              ShowSettingsUtil.getInstance()
                .showSettingsDialog(project, ExperimentalSettingsConfigurable::class.java)
            },
          )
      )
    } else {
      notificationModel.removeNotification(EMBEDDED_EXPERIMENTAL_MESSAGE_KEY)
    }
  }
}

/**
 * Utility function to get [LayoutInspector] from a [Project] Call this only when LayoutInspector
 * needs to be used, see [LayoutInspectorProjectService.getLayoutInspector].
 */
private fun Project.getLayoutInspector(): LayoutInspector {
  return LayoutInspectorProjectService.getInstance(this).getLayoutInspector()
}

private val Content.deviceId: DeviceId?
  get() {
    return (component as? DataProvider)?.getData(DEVICE_ID_KEY.name) as? DeviceId ?: return null
  }
