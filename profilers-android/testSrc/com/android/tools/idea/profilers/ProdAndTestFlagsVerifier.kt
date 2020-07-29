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
package com.android.tools.idea.profilers

import com.android.tools.profilers.FakeIdeProfilerServices
import com.android.tools.profilers.FeatureConfig
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.lang.reflect.Method

/**
 * This test make sure that our flags in prod and test environment are consistent.
 * Any divergence should be documented below with a tracking bug associated.
 */
@RunWith(Parameterized::class)
class ProdAndTestFlagsVerifier(val method: Method, val name: String) {

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "FeatureConfig.{1}()")
    fun data() : Collection<Array<Any>> {
      return FeatureConfig::class.java.declaredMethods.map { arrayOf(it, it.name) }.toList()
    }

    val knownDivergences = mapOf(
      "isCpuApiTracingEnabled" to "b/162491774",
      "isCpuCaptureStageEnabled" to "b/162494041",
      "isCpuNewRecordingWorkflowEnabled" to "b/162493668",
      "isEnergyProfilerEnabled" to "b/162495674",
      "isJniReferenceTrackingEnabled" to "b/162493669",
      "isLiveAllocationsEnabled" to "b/162493670",
      "isNativeMemorySampleEnabled" to "b/162494071",
      "isSeparateHeapDumpUiEnabled" to "b/162493985",
      "isStartupCpuProfilingEnabled" to "b/162493986",
      "isUnifiedPipelineEnabled" to "b/162494995",
      "isUseTraceProcessor" to "b/162495274")
  }

  @Test
  fun checkTestDefaultValues() {
    val prodServicesConfig = IntellijProfilerServices.FeatureConfigProd()
    val prodValue = method.invoke(prodServicesConfig)

    val testServicesConfig = FakeIdeProfilerServices().featureConfig
    val testValue = method.invoke(testServicesConfig)

    if (prodValue != testValue && !knownDivergences.containsKey(name)) {
      Assert.fail("Value for FeatureConfig.$name() is $prodValue in Prod but $testValue for Test.")
    } else if (prodValue == testValue && knownDivergences.containsKey(name)) {
      Assert.fail("Value for FeatureConfig.$name() is the same in Prod and tests ($prodValue), but a divergence is being tracked in " +
                  "${knownDivergences[name]}. If you fixed it, remove it from the knownDivergences.")
    }
  }
}