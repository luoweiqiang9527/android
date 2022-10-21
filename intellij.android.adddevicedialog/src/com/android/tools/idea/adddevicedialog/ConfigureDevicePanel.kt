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
package com.android.tools.idea.adddevicedialog

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import javax.swing.GroupLayout

internal class ConfigureDevicePanel internal constructor() : JBPanel<ConfigureDevicePanel>(null) {
  init {
    val configureDeviceLabel = JBLabel("Configure device")
    val addDeviceToDeviceManagerLabel = JBLabel("Add a device to the device manager")

    val layout = GroupLayout(this)

    val horizontalGroup = layout.createParallelGroup()
      .addComponent(configureDeviceLabel)
      .addComponent(addDeviceToDeviceManagerLabel)

    val verticalGroup = layout.createSequentialGroup()
      .addComponent(configureDeviceLabel)
      .addComponent(addDeviceToDeviceManagerLabel)

    layout.setHorizontalGroup(horizontalGroup)
    layout.setVerticalGroup(verticalGroup)

    setLayout(layout)
  }
}
