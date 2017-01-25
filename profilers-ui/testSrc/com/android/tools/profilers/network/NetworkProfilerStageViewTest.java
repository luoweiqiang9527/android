/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.profilers.network;

import com.android.tools.adtui.SelectionComponent;
import com.android.tools.adtui.TreeWalker;
import com.android.tools.adtui.chart.linechart.LineChart;
import com.android.tools.adtui.swing.FakeKeyboard;
import com.android.tools.adtui.swing.FakeUi;
import com.android.tools.profilers.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;

import static com.google.common.truth.Truth.assertThat;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class NetworkProfilerStageViewTest {
  private FakeUi myFakeUi;
  private StudioProfilersView myView;

  private final FakeNetworkService myService = FakeNetworkService.newBuilder().build();

  @Rule
  public FakeGrpcChannel myGrpcChannel = new FakeGrpcChannel("NetworkProfilerStageViewTestChannel", myService);

  @Before
  public void setUp() {
    StudioProfilers profilers = new StudioProfilers(myGrpcChannel.getClient(), new FakeIdeProfilerServices());
    myView = new StudioProfilersView(profilers, new FakeIdeProfilerComponents());
    JPanel viewComponent = myView.getComponent();
    NetworkProfilerStage stage = new NetworkProfilerStage(profilers);
    profilers.setStage(stage);

    viewComponent.setSize(new Dimension(600, 200));
    myFakeUi = new FakeUi(viewComponent);
  }

  @Test
  public void draggingSelectionOpensConnectionsViewAndPressingEscapeClosesIt() throws Exception {
    NetworkProfilerStageView stageView = (NetworkProfilerStageView)myView.getStageView();

    TreeWalker stageWalker = new TreeWalker(stageView.getComponent());
    LineChart lineChart = (LineChart)stageWalker.descendantStream().filter(LineChart.class::isInstance).findFirst().get();
    SelectionComponent selectionComponent =
      (SelectionComponent)stageWalker.descendantStream().filter(SelectionComponent.class::isInstance).findFirst().get();

    ConnectionsView connectionsView = stageView.getConnectionsView();
    TreeWalker connectionsViewWalker = new TreeWalker(connectionsView.getComponent());
    assertThat(connectionsViewWalker.ancestorStream().allMatch(Component::isVisible)).isFalse();

    Point start = myFakeUi.getPosition(lineChart);
    myFakeUi.mouse.drag(start.x, start.y, 10, 0);
    assertThat(connectionsViewWalker.ancestorStream().allMatch(Component::isVisible)).isTrue();

    myFakeUi.keyboard.setFocus(selectionComponent);
    myFakeUi.keyboard.press(FakeKeyboard.Key.ESC);
    assertThat(connectionsViewWalker.ancestorStream().allMatch(Component::isVisible)).isFalse();
  }
}
