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
package com.android.tools.idea.devicemanagerv2

import com.android.tools.idea.avdmanager.HardwareAccelerationCheck
import com.android.tools.idea.flags.StudioFlags
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Disposer
import org.jetbrains.android.sdk.AndroidSdkUtils

internal class DeviceManagerWelcomeScreenAction : DumbAwareAction() {
  private var deviceManagerWelcomeScreenFrame: DeviceManagerWelcomeScreenFrame? = null

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun update(event: AnActionEvent) {
    val presentation = event.presentation
    if (
      !StudioFlags.UNIFIED_DEVICE_MANAGER_ENABLED.get() ||
        HardwareAccelerationCheck.isChromeOSAndIsNotHWAccelerated() ||
        event.project != null
    ) {
      presentation.isEnabledAndVisible = false
      return
    }
    presentation.isVisible = true
    presentation.isEnabled = AndroidSdkUtils.isAndroidSdkAvailable()
  }

  override fun actionPerformed(event: AnActionEvent) {
    when (val frame = deviceManagerWelcomeScreenFrame) {
      null ->
        deviceManagerWelcomeScreenFrame =
          DeviceManagerWelcomeScreenFrame().also {
            Disposer.register(it) { deviceManagerWelcomeScreenFrame = null }
            it.show()
          }
      else -> frame.getFrame().toFront()
    }
  }
}
