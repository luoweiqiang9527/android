/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.profilers.cpu.config

import com.android.tools.idea.protobuf.Empty
import com.android.tools.profiler.proto.Trace

/**
 * Unspecified configuration used as a placeholder configuration for things like ui objects.
 */
open class UnspecifiedConfiguration(name: String) : ProfilingConfiguration(name) {
  override fun getTraceType(): TraceType {
    return TraceType.UNSPECIFIED
  }

  override fun getRequiredDeviceLevel(): Int {
    return 0
  }

  override fun getOptions(): Empty {
    return Empty.newBuilder().build()
  }

  // Do not add anything. We only check for the presence of options, not the absence.
  override fun addOptions(configBuilder: Trace.TraceConfiguration.Builder, additionalOptions: Map<AdditionalOptions, Any>) {}
}