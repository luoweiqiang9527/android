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
package com.android.tools.idea.streaming

/** Device-specific object providing mirroring functionality. */
interface MirroringHandle {
  /** Indicates if the device is being mirrored or not. */
  val mirroringState: MirroringState

  /** Starts mirroring of the device if the device is being mirrored and stops it if it is active. */
  fun toggleMirroring()
}

enum class MirroringState {
  ACTIVE, INACTIVE
}