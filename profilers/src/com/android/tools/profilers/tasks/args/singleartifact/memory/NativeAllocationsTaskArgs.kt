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
package com.android.tools.profilers.tasks.args.singleartifact.memory

import com.android.tools.profiler.proto.Trace.TraceInfo
import com.android.tools.profilers.memory.HeapProfdSessionArtifact

/**
 * The following class serves as a wrapper for all arguments/data passed to a native allocations task handler.
 */
class NativeAllocationsTaskArgs(private val artifact: HeapProfdSessionArtifact) :
  MemoryCaptureTaskArgs<HeapProfdSessionArtifact, TraceInfo>() {
  override fun getMemoryCaptureArtifact() = artifact
}