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
package com.android.tools.idea.vitals.ui

import com.android.tools.idea.insights.TimeIntervalFilter

internal val VitalsTimeIntervals =
  listOf(
    TimeIntervalFilter.ONE_DAY,
    TimeIntervalFilter.SEVEN_DAYS,
    TimeIntervalFilter.FOURTEEN_DAYS,
    TimeIntervalFilter.TWENTY_EIGHT_DAYS,
    TimeIntervalFilter.SIXTY_DAYS
  )
