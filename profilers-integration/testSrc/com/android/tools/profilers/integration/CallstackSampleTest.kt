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
package com.android.tools.profilers.integration

import com.android.tools.asdriver.tests.Emulator
import org.junit.Test

class CallstackSampleTest : ProfilersTestBase() {

  /**
   * Validate system trace is working.
   * <p>
   * This is run to qualify releases. Please involve the test team in substantial changes.
   * <p>
   * TT ID: a70d00da-9126-4b4e-8803-cab86d010007
   * <p>
   *   <pre>
   *   Test Steps:
   *   1. Import minapp in the testData directory of this module.
   *   2. Start profile 'app' with complete data (using API 33 Play Tiramisu System image).
   *   3. Record callstack sample.
   *   4. Stop cpu capture.
   *   5. Stop profile session.
   *   Verify:
   *   1. Verify logs for starting complete data.
   *   2. Verify logs for starting and stopping callStack sample.
   *   4. Verify UI components while call stack sampling is being recorded.
   *   5. Verify UI components after call stack sampling is completed.
   *   5. Verify if the session is stopped.
   *   </pre>
   * <p>
   */
  @Test
  fun testRecordCallstackSample() {
    profileApp(
      systemImage = Emulator.SystemImage.API_33_PlayStore, // Provides more stability than API 29
      testFunction = { studio, _ ->
        // TODO(b/260867011): Remove the wait, once there is a definitive way to tell that the emulator is ready to deploy the app.
        println("Waiting for 20 seconds before running the app so that the emulator is ready")
        Thread.sleep(20000)

        profileWithCompleteData(studio)

        verifyIdeaLog(".*PROFILER\\:\\s+Session\\s+started.*support\\s+level\\s+\\=DEBUGGABLE\$", 300)
        verifyIdeaLog(".*StudioMonitorStage.*PROFILER\\:\\s+Enter\\s+StudioMonitorStage\$", 120)
        // Waiting for UI Component specific to profiling with complete data
        studio.waitForComponentByClass("TooltipLayeredPane", "InstructionsPanel", "InstructionsComponent")

        startCallstackSample(studio)

        verifyIdeaLog(".*PROFILER\\:\\s+CPU\\s+capture\\s+start\\s+attempted\$", 120)

        studio.waitForComponentByClass("TooltipLayeredPane", "RecordingOptionsView", "FlexibleGrid", "ProfilerCombobox")
        studio.waitForComponentByClass("TooltipLayeredPane", "HideablePanel", "CpuListScrollPane", "DragAndDropList")

        verifyIdeaLog(".*PROFILER\\:\\s+CPU\\s+capture\\s+start\\s+succeeded\$", 120)

        // To help stabilize the test.
        Thread.sleep(5000)

        stopCpuCapture(studio)

        verifyIdeaLog(".*PROFILER\\:\\s+CPU\\s+capture\\s+stop\\s+attempted\$", 120)
        verifyIdeaLog(".*PROFILER\\:\\s+CPU\\s+capture\\s+stop\\s+succeeded\$", 300)

        // Verify if the cpu capture is parsed.
        verifyIdeaLog(".*PROFILER\\:\\s+CPU\\s+capture\\s+parse\\s+succeeded\$", 300)
        // TODO(b/302155473): Missing log verification to make sure the callstack sample is parsed successfully.

        studio.waitForComponentByClass("TooltipLayeredPane", "CpuAnalysisSummaryTab", "UsageInstructionsView")

        stopProfilingSession(studio)
      }
    )
  }
}