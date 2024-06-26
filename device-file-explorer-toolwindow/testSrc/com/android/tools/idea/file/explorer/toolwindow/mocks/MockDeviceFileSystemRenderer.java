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
package com.android.tools.idea.file.explorer.toolwindow.mocks;

import com.android.tools.idea.file.explorer.toolwindow.fs.DeviceFileSystem;
import com.android.tools.idea.file.explorer.toolwindow.fs.DeviceFileSystemRenderer;
import com.intellij.ui.SimpleListCellRenderer;
import javax.swing.ListCellRenderer;
import org.jetbrains.annotations.NotNull;

public class MockDeviceFileSystemRenderer<S extends DeviceFileSystem> implements DeviceFileSystemRenderer<S> {
  private final ListCellRenderer<S> myRenderer;

  public MockDeviceFileSystemRenderer() {
    myRenderer = SimpleListCellRenderer.create("<No device>", DeviceFileSystem::getName);
  }

  @NotNull
  @Override
  public ListCellRenderer<S> getDeviceNameListRenderer() {
    return myRenderer;
  }
}
