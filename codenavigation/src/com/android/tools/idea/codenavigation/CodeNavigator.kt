/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.codenavigation

import java.util.concurrent.CompletableFuture

/**
 * Base class for a service responsible for handling navigations to target [CodeLocation]s,
 * as well as registering and triggering listeners interested in the event.
 */
abstract class CodeNavigator {
  private val myListeners = mutableListOf<Listener>()

  fun addListener(listener: Listener) {
    myListeners.add(listener)
  }

  fun removeListener(listener: Listener) {
    myListeners.remove(listener)
  }

  fun navigate(location: CodeLocation): CompletableFuture<Boolean> {
    myListeners.forEach{ it.onNavigated(location) }
    return handleNavigate(location)
  }

  abstract fun isNavigatable(location: CodeLocation): CompletableFuture<Boolean>

  protected abstract fun handleNavigate(location: CodeLocation): CompletableFuture<Boolean>

  interface Listener {
    fun onNavigated(location: CodeLocation)
  }
}