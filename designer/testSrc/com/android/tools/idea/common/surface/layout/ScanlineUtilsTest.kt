/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.idea.common.surface.layout

import com.android.tools.idea.uibuilder.surface.TestSceneView
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanlineUtilsTest {
  @Test
  fun testScanlineMapping() {
    /*
     * Not to scale. Diagram of the SceneView representation below.
     *
     * S -> Start scanline
     * E -> End scanline
     *
     * H Scanlines
     *
     *          100, 101
     * S101>       +--------------+
     *             |              |
     *             |              |
     * E112>       +--------------+
     *                        110, 112
     *
     *                                  200, 201
     * S202>                              +--------------+
     *                                    |              |
     *                                    |              |
     * E214>                              +--------------+
     *                                            212, 214
     *
     *
     *             ^              ^       ^              ^          V Scanlines
     *          S100           E110    S200           E212
     */

    val sceneViews = listOf(
      TestSceneView(10, 11).apply { setLocation(100, 101) },
      TestSceneView(12, 13).apply { setLocation(200, 201) })

    // Verify start vertical scanlines
    assertArrayEquals(arrayOf(100, 200), sceneViews.findAllScanlines { it.x }.toTypedArray())
    // Verify start horizontal scanlines
    assertArrayEquals(arrayOf(101, 201), sceneViews.findAllScanlines { it.y }.toTypedArray())
    // Verify end vertical scanlines
    assertArrayEquals(arrayOf(110, 212), sceneViews.findAllScanlines { it.x + it.getSize(null).width }.toTypedArray())
    // Verify end horizontal scanlines
    assertArrayEquals(arrayOf(112, 214), sceneViews.findAllScanlines { it.y + it.getSize(null).height }.toTypedArray())
  }

  @Test
  fun testSmallerScanline() {
    val scanlines = listOf(5, 10, 15, 30)
    assertEquals(5, findSmallerScanline(scanlines, 5, -1))
    assertEquals(5, findSmallerScanline(scanlines, 6, -1))
    assertEquals(10, findSmallerScanline(scanlines, 11, -1))
    assertEquals(30, findSmallerScanline(scanlines, 500000, -1))
    assertEquals(-1, findSmallerScanline(scanlines, 4, -1))
  }

  @Test
  fun testLargerScanline() {
    val scanlines = listOf(5, 10, 15, 30)
    assertEquals(5, findLargerScanline(scanlines, 5, -1))
    assertEquals(5, findLargerScanline(scanlines, 1, -1))
    assertEquals(5, findLargerScanline(scanlines, -100000, -1))
    assertEquals(15, findLargerScanline(scanlines, 11, -1))
    assertEquals(-1, findLargerScanline(scanlines, 31, -1))
  }
}