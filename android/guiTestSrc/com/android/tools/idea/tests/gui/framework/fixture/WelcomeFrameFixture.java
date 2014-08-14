/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.tools.idea.tests.gui.framework.fixture;

import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import org.fest.swing.core.Robot;
import org.fest.swing.exception.ComponentLookupException;
import org.fest.swing.fixture.ComponentFixture;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class WelcomeFrameFixture extends ComponentFixture<WelcomeFrame> {
  @NotNull
  public static WelcomeFrameFixture find(@NotNull Robot robot) {
    for (Frame frame : Frame.getFrames()) {
      if (frame instanceof WelcomeFrame && frame.isShowing()) {
        return new WelcomeFrameFixture(robot, (WelcomeFrame)frame);
      }
    }
    throw new ComponentLookupException("Unable to find 'Welcome' window");
  }

  private WelcomeFrameFixture(@NotNull Robot robot, @NotNull WelcomeFrame target) {
    super(robot, target);
  }

  @NotNull
  public ActionButtonFixture newProjectButton() {
    return findActionButtonByActionId("WelcomeScreen.ImportProject");
  }

  @NotNull
  public ActionButtonFixture importProjectButton() {
    return findActionButtonByActionId("WelcomeScreen.ImportProject");
  }

  @NotNull
  public ActionButtonFixture openProjectButton() {
    String actionId = "WelcomeScreen.OpenProject";
    return findActionButtonByActionId(actionId);
  }

  @NotNull
  private ActionButtonFixture findActionButtonByActionId(String actionId) {
    return ActionButtonFixture.findByActionId(actionId, robot, target);
  }
}
