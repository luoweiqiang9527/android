/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.ui.resourcechooser.colorpicker2

import java.awt.*
import javax.swing.JComponent

class ColorIndicator(color: Color = DEFAULT_PICKER_COLOR) : JComponent() {

  var color = color
    set(value) {
      field = value
      repaint()
    }

  override fun paintComponent(g: Graphics) {
    (g as Graphics2D).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val originalColor = g.color
    g.color = color
    g.fillOval(0 + insets.left, 0 + insets.top, width - insets.left - insets.right, height - insets.top - insets.bottom)
    g.color = originalColor
  }
}
