/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.tests.gui.framework.fixture.npw;

import com.android.tools.idea.tests.gui.framework.GuiTests;
import com.android.tools.idea.tests.gui.framework.fixture.newProjectWizard.AbstractWizardFixture;
import com.android.tools.idea.tests.gui.framework.matcher.Matchers;
import org.fest.swing.core.Robot;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NewActivityWizardFixture extends AbstractWizardFixture<NewActivityWizardFixture> {

  private NewActivityWizardFixture(@NotNull Robot robot, @NotNull JDialog target) {
    super(NewActivityWizardFixture.class, robot, target);
  }

  @NotNull
  public static NewActivityWizardFixture find(@NotNull Robot robot) {
    JDialog dialog = GuiTests.waitUntilShowing(robot, Matchers.byTitle(JDialog.class, "New Android Activity"));
    return new NewActivityWizardFixture(robot, dialog);
  }

  @NotNull
  public ConfigureBasicActivityStepFixture getConfigureActivityStep() {
    JRootPane rootPane = findStepWithTitle("Configure Activity");
    return new ConfigureBasicActivityStepFixture(robot(), rootPane);
  }
}
