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
package com.android.build.attribution.ui.view

import com.android.build.attribution.analyzers.DownloadsAnalyzer
import com.android.build.attribution.ui.mockDownloadsData
import com.android.build.attribution.ui.model.DownloadsInfoPageModel
import com.android.tools.adtui.TreeWalker
import com.google.common.truth.Truth
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.RunsInEdt
import com.intellij.ui.OnePixelSplitter
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class DownloadsInfoPageViewTest {

  @Test
  fun testViewCreatedWithNonEmptyData() {
    val downloadsData = mockDownloadsData()
    val mockHandlers = Mockito.mock(ViewActionHandlers::class.java)
    val pageModel = DownloadsInfoPageModel(downloadsData)
    val downloadsPage = DownloadsInfoPageView(pageModel, mockHandlers)

    val splitter = TreeWalker(downloadsPage.component).descendants().filterIsInstance<OnePixelSplitter>().single()
    Truth.assertThat(splitter.firstComponent).isNotNull()
    Truth.assertThat(splitter.secondComponent).isNotNull()
  }

  @Test
  fun testViewCreatedWithEmptyData() {
    val downloadsData = DownloadsAnalyzer.ActiveResult(emptyList())
    val mockHandlers = Mockito.mock(ViewActionHandlers::class.java)
    val pageModel = DownloadsInfoPageModel(downloadsData)
    val downloadsPage = DownloadsInfoPageView(pageModel, mockHandlers)

    val splitter = TreeWalker(downloadsPage.component).descendants().filterIsInstance<OnePixelSplitter>().single()
    Truth.assertThat(splitter.firstComponent).isNotNull()
    Truth.assertThat(splitter.secondComponent).isNull()

  }
}