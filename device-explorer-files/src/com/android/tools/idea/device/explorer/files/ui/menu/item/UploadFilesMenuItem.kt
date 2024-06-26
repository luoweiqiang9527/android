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
package com.android.tools.idea.device.explorer.files.ui.menu.item

import com.android.tools.idea.device.explorer.files.DeviceFileEntryNode
import com.android.tools.idea.device.explorer.files.ui.DeviceFileExplorerActionListener
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.Shortcut
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.Icon
import javax.swing.KeyStroke

class UploadFilesMenuItem(
  listener: DeviceFileExplorerActionListener,
  private val context: MenuContext
) : SingleSelectionTreeMenuItem(listener) {
  override fun getText(nodes: List<DeviceFileEntryNode>): String = "Upload..."

  override val icon: Icon
    get() = AllIcons.Actions.Upload

  override val shortcuts: Array<Shortcut?>
    get() = arrayOf(
      KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK), null),
      KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK), null))

  override val isVisible: Boolean
    get() =
      if (context == MenuContext.Toolbar) true else super.isVisible

  override fun isVisible(node: DeviceFileEntryNode): Boolean =
    node.entry.isDirectory || node.isSymbolicLinkToDirectory

  override fun run(node: DeviceFileEntryNode) {
    listener.uploadFile(node)
  }
}