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
package com.android.tools.idea.res;

import com.android.annotations.concurrency.GuardedBy;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResourceRepositories implements Disposable {
  private static final Key<ResourceRepositories> KEY = Key.create(ResourceRepositories.class.getName());

  private static final Object APP_RESOURCES_LOCK = new Object();
  private static final Object PROJECT_RESOURCES_LOCK = new Object();

  private AndroidFacet myFacet;

  @GuardedBy("APP_RESOURCES_LOCK")
  private AppResourceRepository myAppResources;

  @GuardedBy("PROJECT_RESOURCES_LOCK")
  private ProjectResourceRepository myProjectResources;

  @NotNull
  public static ResourceRepositories getOrCreateInstance(@NotNull AndroidFacet facet) {
    ResourceRepositories repositories = facet.getUserData(KEY);
    if (repositories == null) {
      repositories = new ResourceRepositories(facet);
      facet.putUserData(KEY, repositories);
    }
    return repositories;
  }

  private ResourceRepositories(@NotNull AndroidFacet facet) {
    myFacet = facet;
    Disposer.register(facet, this);
  }

  @Contract("true -> !null")
  @Nullable
  AppResourceRepository getAppResources(boolean createIfNecessary) {
    AndroidFacet facet = myFacet;
    synchronized (APP_RESOURCES_LOCK) {
      if (myAppResources == null && createIfNecessary) {
        myAppResources = AppResourceRepository.create(facet);
        Disposer.register(this, myAppResources);
      }
      return myAppResources;
    }
  }

  @Contract("true -> !null")
  @Nullable
  ProjectResourceRepository getProjectResources(boolean createIfNecessary) {
    AndroidFacet facet = myFacet;
    synchronized (PROJECT_RESOURCES_LOCK) {
      if (myProjectResources == null && createIfNecessary) {
        myProjectResources = ProjectResourceRepository.create(facet);
        Disposer.register(this, myProjectResources);
      }
      return myProjectResources;
    }
  }

  public void refreshResources() {
    synchronized (PROJECT_RESOURCES_LOCK) {
      if (myProjectResources != null) {
        Disposer.dispose(myProjectResources);
        myProjectResources = null;
      }
    }

    synchronized (APP_RESOURCES_LOCK) {
      if (myAppResources != null) {
        Disposer.dispose(myAppResources);
        myAppResources = null;
      }
    }
  }

  @Override
  public void dispose() {
    myFacet.putUserData(KEY, this);
    myFacet = null;
  }
}
