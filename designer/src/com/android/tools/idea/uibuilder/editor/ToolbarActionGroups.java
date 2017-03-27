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
package com.android.tools.idea.uibuilder.editor;

import com.android.tools.idea.uibuilder.surface.NlDesignSurface;
import com.intellij.openapi.actionSystem.ActionGroup;
import org.jetbrains.annotations.NotNull;

public class ToolbarActionGroups {
  final NlDesignSurface mySurface;

  public ToolbarActionGroups(@NotNull NlDesignSurface surface) {
    mySurface = surface;
  }

  @NotNull
  ActionGroup getNorthGroup() {
    return ActionGroup.EMPTY_GROUP;
  }

  @NotNull
  ActionGroup getEastGroup() {
    return ActionGroup.EMPTY_GROUP;
  }
}
