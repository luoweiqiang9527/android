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

import com.android.tools.idea.gradle.dsl.api.dependencies.DependencyModel;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class PsDependency extends PsChildModel implements PsBaseDependency {
  @NotNull private final Set<DependencyModel> myParsedModels = Sets.newLinkedHashSet();

  protected PsDependency(@NotNull PsModule parent, @NotNull Collection<? extends DependencyModel> parsedModels) {
    super(parent);
    myParsedModels.addAll(parsedModels);
  }

  @Override
  @NotNull
  public String getJoinedConfigurationNames() {
    List<String> configurationNames = getConfigurationNames();
    int count = configurationNames.size();
    if (count == 1) {
      return configurationNames.get(0);
    }
    else if (count > 1) {
      return Joiner.on(", ").join(configurationNames);
    }
    return "";
  }

  @NotNull
  public List<String> getConfigurationNames() {
    if (myParsedModels.isEmpty()) {
      return Collections.emptyList();
    }
    return myParsedModels.stream().map(DependencyModel::configurationName).distinct().collect(Collectors.toList());
  }

  @Override
  public boolean isDeclared() {
    return !myParsedModels.isEmpty();
  }

  public void addParsedModel(@NotNull DependencyModel parsedModel) {
    myParsedModels.add(parsedModel);
  }

  @NotNull
  public ImmutableCollection<DependencyModel> getParsedModels() {
    return myParsedModels.isEmpty() ? ImmutableSet.of() : ImmutableSet.copyOf(myParsedModels);
  }

  public enum TextType {
    PLAIN_TEXT, FOR_NAVIGATION
  }
}
