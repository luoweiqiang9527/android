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
package com.android.tools.idea.gradle.structure.model;

import com.android.tools.idea.gradle.dsl.model.GradleBuildModel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class PsModule extends PsChildModel {
  @Nullable private String myGradlePath;

  // Module can be null in the case of new modules created in the PSD.
  @Nullable private final Module myResolvedModel;

  private boolean myInitParsedModel;
  private GradleBuildModel myParsedModel;
  private String myModuleName;

  protected PsModule(@NotNull PsProject parent,
                     @NotNull Module resolvedModel,
                     @NotNull String moduleGradlePath) {
    super(parent);
    myResolvedModel = resolvedModel;
    myGradlePath = moduleGradlePath;
    myModuleName = resolvedModel.getName();
  }

  protected PsModule(@NotNull PsProject parent, @NotNull String name) {
    super(parent);
    myResolvedModel = null;
    myModuleName = name;
  }

  @Override
  @NotNull
  public PsProject getParent() {
    return (PsProject)super.getParent();
  }

  @Override
  @NotNull
  public String getName() {
    return myModuleName;
  }

  @Override
  public boolean isDeclared() {
    return myParsedModel != null;
  }

  @Nullable
  public GradleBuildModel getParsedModel() {
    if (!myInitParsedModel) {
      myInitParsedModel = true;
      if (myResolvedModel != null) {
        myParsedModel = GradleBuildModel.get(myResolvedModel);
      }
    }
    return myParsedModel;
  }

  @Nullable
  public String getGradlePath() {
    return myGradlePath;
  }

  @Override
  @Nullable
  public Module getResolvedModel() {
    return myResolvedModel;
  }

  @Override
  public Icon getIcon() {
    return AllIcons.Nodes.Module;
  }
}
