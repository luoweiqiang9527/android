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
package com.android.tools.profilers.com.android.tools.profilers

import com.android.sdklib.AndroidVersion
import com.android.testutils.MockitoKt.whenever
import com.android.tools.adtui.AxisComponent
import com.android.tools.adtui.RangeTooltipComponent
import com.android.tools.adtui.TreeWalker
import com.android.tools.adtui.model.FakeTimer
import com.android.tools.adtui.stdui.CommonButton
import com.android.tools.adtui.stdui.TimelineScrollbar
import com.android.tools.idea.transport.faketransport.FakeGrpcChannel
import com.android.tools.idea.transport.faketransport.FakeTransportService
import com.android.tools.profiler.proto.Common
import com.android.tools.profilers.FakeIdeProfilerComponents
import com.android.tools.profilers.FakeIdeProfilerServices
import com.android.tools.profilers.LiveStage
import com.android.tools.profilers.LiveStageView
import com.android.tools.profilers.ProfilerClient
import com.android.tools.profilers.ProfilerColors
import com.android.tools.profilers.SessionProfilersView
import com.android.tools.profilers.StudioProfilers
import com.android.tools.profilers.StudioProfilersView
import com.android.tools.profilers.SupportLevel
import com.android.tools.profilers.cpu.CpuUsageView
import com.android.tools.profilers.event.FakeEventService
import com.android.tools.profilers.memory.FlexibleLegendPanel
import com.google.common.truth.Truth.assertThat
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.EdtRule
import com.intellij.ui.components.JBLabel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import javax.swing.JComponent
import javax.swing.JPanel

class LiveStageViewTest {
  private val myTimer = FakeTimer()
  private val myComponents = FakeIdeProfilerComponents()
  private val myIdeServices = FakeIdeProfilerServices()
  private val myTransportService = FakeTransportService(myTimer, true, AndroidVersion.VersionCodes.S,
                                                        Common.Process.ExposureLevel.PROFILEABLE)

  @get:Rule
  val myGrpcChannel = FakeGrpcChannel("LiveStageViewTest", myTransportService, FakeEventService())

  @get:Rule
  val myEdtRule = EdtRule()

  @get:Rule
  val applicationRule = ApplicationRule()

  @get:Rule
  val disposableRule = DisposableRule()

  private lateinit var myProfilersView: StudioProfilersView
  private lateinit var myStage: LiveStage

  @Before
  fun setUp() {
    val profilers = Mockito.spy(StudioProfilers(ProfilerClient(myGrpcChannel.channel), myIdeServices, myTimer))
    profilers.setPreferredProcess(FakeTransportService.FAKE_DEVICE_NAME, FakeTransportService.FAKE_PROCESS_NAME, null)
    Mockito.doReturn(SupportLevel.PROFILEABLE).whenever(profilers).selectedSessionSupportLevel
    // One second must be enough for new devices (and processes) to be picked up
    myTimer.tick(FakeTimer.ONE_SECOND_IN_NS)
    myStage = LiveStage(profilers)
    myStage.enter()
    myProfilersView = SessionProfilersView(profilers, myComponents, disposableRule.disposable)
  }

  private fun getJPanels() : List<JPanel> {
    val liveStageView = LiveStageView(myProfilersView, myStage)
    val treeWalker = TreeWalker(liveStageView.component)
    return treeWalker.descendants().filterIsInstance(JPanel::class.java)
  }

  private fun getTreeWalkerTopPanel(jPanels : List<JPanel>) : TreeWalker {
    // First from JPanel should be top panel
    val mainPanel = jPanels[0]
    val treeWalkerMainPanel = TreeWalker(mainPanel)
    val mainPanelComponents = treeWalkerMainPanel.descendants().filterIsInstance(JPanel::class.java)

    val topPanel = mainPanelComponents[1]
    return TreeWalker(topPanel)
  }

  @Test
  fun testTooltipIsPresentUnderStageViewPanel() {
    val liveStageView = LiveStageView(myProfilersView, myStage)
    val treeWalker = TreeWalker(liveStageView.component)
    val tooltipComponent = treeWalker.descendants().filterIsInstance(RangeTooltipComponent::class.java)
    // Check for tooltip presence in live view component
    assertThat(tooltipComponent.size).isEqualTo(1)

    val jPanels = treeWalker.descendants().filterIsInstance(JPanel::class.java)
    // Lot of JPanel will be there, since we are adding many sub live views.
    assertThat(jPanels.size).isGreaterThan(1)
  }

  /** Test main panel of LiveStage view has topPanel, which is a tabular layout **/
  @Test
  fun testLiveStageViewTopPanel() {
    val jPanels = getJPanels()
    // Many JPanel to be present, since we are adding many sub live views.
    assertThat(jPanels.size).isGreaterThan(1)

    // First from JPanel should be top panel
    val mainPanel = jPanels[0]
    assertThat(mainPanel.background).isEqualTo(ProfilerColors.DEFAULT_BACKGROUND)
    val treeWalkerMainPanel = TreeWalker(mainPanel)
    val mainPanelComponents = treeWalkerMainPanel.descendants().filterIsInstance(JPanel::class.java)
    // Main panel has many child JPanel
    assertThat(mainPanelComponents.size).isGreaterThan(1)

    val topPanel = mainPanelComponents[1]
    assertThat(topPanel.background).isEqualTo(ProfilerColors.DEFAULT_STAGE_BACKGROUND)

    val treeWalkerTopPanel = TreeWalker(topPanel)
    val topPanelComponents = treeWalkerTopPanel.descendants().filterIsInstance(JPanel::class.java)
    // Top panel has many child JPanel
    assertThat(topPanelComponents.size).isGreaterThan(1)
  }

  @Test
  fun testTopPanelHasTimeAxis() {
    val topPanelAxisComponents = getTreeWalkerTopPanel(getJPanels()).descendants().filterIsInstance(AxisComponent::class.java)
    assertThat(topPanelAxisComponents.size).isGreaterThan(0)
  }

  @Test
  fun testTopPanelStopRecordingButton() {
    val stageView = LiveStageView(myProfilersView, myStage)
    val result = stageView.toolbar
    val topLevelToolBar = TreeWalker(result.components[0])
    val topPanelAxisComponents = topLevelToolBar.descendants().filterIsInstance(CommonButton::class.java)
    assertThat(topPanelAxisComponents.size).isGreaterThan(0)
    val stopRecordingButton = topPanelAxisComponents.stream().filter{i -> "Stop Recording" == i.toolTipText }.findAny()
    assertThat(stopRecordingButton.isPresent).isTrue()
  }

  @Test
  fun testTopPanelHasScrollBar() {
    val topPanelAxisComponents = getTreeWalkerTopPanel(getJPanels()).descendants().filterIsInstance(TimelineScrollbar::class.java)
    assertThat(topPanelAxisComponents.size).isEqualTo(1)
  }

  /** Test main panel has top panel, top panel has the liveViews as child **/
  @Test
  fun testLiveStageViewPanel() {
    val topPanelComponents = getTreeWalkerTopPanel(getJPanels()).descendants().filterIsInstance(JPanel::class.java)
    // Top panel has many child JPanel
    assertThat(topPanelComponents.size).isGreaterThan(2)

    val viewPanel = topPanelComponents[2]
    assertThat(viewPanel.background).isEqualTo(ProfilerColors.DEFAULT_BACKGROUND)

    val treeWalkerViewPanel = TreeWalker(viewPanel)
    val viewPanelComponents = treeWalkerViewPanel.descendants().filterIsInstance(JPanel::class.java)
    // View panel has many child JPanel
    assertThat(viewPanelComponents.size).isGreaterThan(1)
  }

  /** Test View Panel has CPU Live view **/
  @Test
  fun testLiveStageCpuLiveView() {
    val topPanelComponents = getTreeWalkerTopPanel(getJPanels()).descendants().filterIsInstance(JPanel::class.java)
    // Top panel has many child JPanel
    assertThat(topPanelComponents.size).isGreaterThan(1)

    val viewPanel = topPanelComponents[2]
    assertThat(viewPanel.background).isEqualTo(ProfilerColors.DEFAULT_BACKGROUND)

    val treeWalkerViewPanel = TreeWalker(viewPanel)
    val viewPanelComponents = treeWalkerViewPanel.descendants().filterIsInstance(JPanel::class.java)
    assertThat(topPanelComponents.size).isGreaterThan(1)

    val cpuLiveView = viewPanelComponents[1]
    val treeWalkerCpuLiveView = TreeWalker(cpuLiveView)
    val cpuUsageViewComponents = treeWalkerCpuLiveView.descendants().filterIsInstance(CpuUsageView::class.java)
    // View panel has many child JPanel
    assertThat(cpuUsageViewComponents.size).isEqualTo(1)
  }

  /** Testing View Panel has Memory Live view **/
  @Test
  fun testLiveStageMemoryLiveView() {
    val topPanelComponents = getTreeWalkerTopPanel(getJPanels()).descendants().filterIsInstance(JPanel::class.java)
    // Top panel has many child JPanel
    assertThat(topPanelComponents.size).isGreaterThan(2)

    val viewPanel = topPanelComponents[2]
    assertThat(viewPanel.background).isEqualTo(ProfilerColors.DEFAULT_BACKGROUND)

    val treeWalkerViewPanel = TreeWalker(viewPanel)
    // Checking for the FlexibleLegendPanel which is part of memory
    val memoryComponent = treeWalkerViewPanel.descendants().filterIsInstance(FlexibleLegendPanel::class.java)
    assertThat(memoryComponent.size).isEqualTo(1)
  }

  @Test
  fun testHasContextMenuItems() {
    myComponents.clearContextMenuItems()
    LiveStageView(myProfilersView, myStage)
    val items = myComponents.allContextMenuItems
    // 3 section of context menu
    // 5 for cpu usage, 5 for cpu threads, 8 for memory
    assertThat(items.size).isEqualTo(18)
  }

  @Test
  fun testToolbar() {
    val stageView = LiveStageView(myProfilersView, myStage)
    val result = stageView.toolbar
    val topLevelToolBar = TreeWalker(result.components[0])
    val componentsInToolbar = topLevelToolBar.descendants().filterIsInstance(JComponent::class.java)
    // Toolbar should have elements
    assertThat(componentsInToolbar.size).isGreaterThan(0)

    var toolbar = result.getComponent(0) as JPanel
    // 1st item is garbage collection and 2nd item is stop recording button
    assertThat(toolbar.components).asList().hasSize(2)

    val firstElementInToolbar = toolbar.components[0]

    // Verify garbage collection button is there in toolbar
    val garbageCollectionButtonInToolbar = TreeWalker(firstElementInToolbar).descendants().filterIsInstance(CommonButton::class.java)
    assertThat(garbageCollectionButtonInToolbar.size).isEqualTo(1)

    val secondElementInToolbar = toolbar.components[0]

    // Verify stop recording button is there in toolbar
    val stopRecordingButtonInToolbar = TreeWalker(secondElementInToolbar).descendants().filterIsInstance(CommonButton::class.java)
    assertThat(stopRecordingButtonInToolbar.size).isEqualTo(1)
  }

  @Test
  fun testDefaultMessage() {
    val topPanelComponents = getTreeWalkerTopPanel(getJPanels()).descendants().filterIsInstance(JPanel::class.java)
    // Top panel has many child JPanel
    assertThat(topPanelComponents.size).isGreaterThan(2)

    val viewPanel = topPanelComponents[1]
    val treeWalkerViewPanel = TreeWalker(viewPanel)
    val viewPanelComponents = treeWalkerViewPanel.descendants().filterIsInstance(JPanel::class.java)
    val secondElementInToolbar = viewPanelComponents[0]

    val messageSectionComponentsLabel = TreeWalker(secondElementInToolbar).descendants().filterIsInstance(JBLabel::class.java)

    //Default mode is PROFILEABLE, so check for that message
    assertThat(messageSectionComponentsLabel[0].text).isEqualTo("Profiling with low overhead. " +
                                                                "Certain profiler features will be unavailable in this mode.")
    assertThat(messageSectionComponentsLabel[1].text).isEqualTo("Dismiss")
    assertThat(messageSectionComponentsLabel[2].text).isEqualTo("Learn more")
  }
}