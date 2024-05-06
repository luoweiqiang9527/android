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
package com.android.tools.profilers

import com.intellij.openapi.diagnostic.Logger

/**
 * This interface will be implemented by stages that are used as an intermediate stage before entering a terminal stage. One example
 * is the CpuProfilerStage. It is an InterimStage as it serves as the intermediate stage between starting and displaying a CPU trace.
 */
interface InterimStage {
  private val LOGGER: Logger
    get() = Logger.getInstance(InterimStage::class.java)

  /**
   * To be passed in from the class creating the InterimStage instance, customizing the behavior on stoppage of this stage's facilitated
   * procedure.
   */
  val stopAction: Runnable

  /**
   * Method used to safely invoke the "stopAction".
   *
   * This now allows the implementing Stage's bound view to invoke the stop action passed by a parent class.
   */
  fun stop() {
    try {
      stopAction.run()
    } catch (e: Exception) {
      LOGGER.error(e.message.toString())
    }
  }
}