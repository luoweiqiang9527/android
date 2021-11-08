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
package com.android.tools.idea.compose.preview.animation

import com.android.tools.idea.testing.AndroidProjectRule
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.awt.Graphics2D
import javax.swing.JSlider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InspectorPainterTest {

  @get:Rule
  val projectRule = AndroidProjectRule.inMemory()

  @Test
  fun zeroWidth() {
    val slider = mock(JSlider::class.java)
    Mockito.`when`(slider.width).thenReturn(0)
    Mockito.`when`(slider.maximum).thenReturn(200)
    Mockito.`when`(slider.minimum).thenReturn(0)
    val result = InspectorPainter.Slider.getTickIncrement(slider, 100)
    assertEquals(200, result)
  }

  @Test
  fun maxIntMaximum() {
    val slider = mock(JSlider::class.java)
    Mockito.`when`(slider.width).thenReturn(300)
    Mockito.`when`(slider.maximum).thenReturn(Int.MAX_VALUE)
    Mockito.`when`(slider.minimum).thenReturn(0)
    val result = InspectorPainter.Slider.getTickIncrement(slider, 100)
    assertEquals(700_000_000, result)
  }

  @Test
  fun largeMaximum() {
    val slider = mock(JSlider::class.java)
    Mockito.`when`(slider.width).thenReturn(300)
    Mockito.`when`(slider.maximum).thenReturn(200_000_000)
    Mockito.`when`(slider.minimum).thenReturn(0)
    val result = InspectorPainter.Slider.getTickIncrement(slider, 100)
    assertEquals(60_000_000, result)
  }

  @Test
  fun smallMaximum() {
    val slider = mock(JSlider::class.java)
    Mockito.`when`(slider.width).thenReturn(300)
    Mockito.`when`(slider.maximum).thenReturn(5)
    Mockito.`when`(slider.minimum).thenReturn(0)
    val result = InspectorPainter.Slider.getTickIncrement(slider, 100)
    assertEquals(1, result)
  }

  @Test
  fun paintThumbForHorizSlider() {
    val g = mock(Graphics2D::class.java)
    InspectorPainter.Thumb.paintThumbForHorizSlider(
      g, 0, 0, 100)
  }

  @Test
  fun createStartEndComboBox() {
    val comboBox: InspectorPainter.StateComboBox =
      InspectorPainter.StartEndComboBox(logger = {}, callback = {})
    assertNotNull(comboBox.stateHashCode())
    assertNotNull(comboBox.component)
    comboBox.updateStates(setOf("One", "Two", "Three"))
    // Before `setStartState` is called, states are the same.
    assertEquals("One", comboBox.getState(0))
    assertEquals("One", comboBox.getState(1))
    comboBox.setStartState("One")
    assertEquals("One", comboBox.getState(0))
    assertEquals("Two", comboBox.getState(1))
    comboBox.setupListeners()
  }

  @Test
  fun createAnimatedVisibilityComboBox() {
    val comboBox: InspectorPainter.StateComboBox =
      InspectorPainter.AnimatedVisibilityComboBox(logger = {}, callback = {})
    assertNotNull(comboBox.stateHashCode())
    assertNotNull(comboBox.component)
    comboBox.updateStates(setOf("One", "Two", "Three"))
    assertEquals("One", comboBox.getState(0))
    comboBox.setStartState("Two")
    assertEquals("Two", comboBox.getState(0))
    comboBox.setupListeners()
  }

  @Test
  fun createEmptyComboBox() {
    val comboBox: InspectorPainter.StateComboBox =
      InspectorPainter.EmptyComboBox()
    assertNotNull(comboBox.component)
  }
}