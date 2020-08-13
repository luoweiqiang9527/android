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
package com.android.tools.idea.profilers.perfetto.traceprocessor

import com.android.testutils.TestUtils
import com.android.tools.profilers.FakeFeatureTracker
import com.android.tools.profilers.cpu.CpuProfilerTestUtils
import com.android.tools.profilers.cpu.atrace.SystemTraceSurfaceflingerManager
import com.android.tools.profilers.systemtrace.ProcessModel
import com.android.tools.profilers.systemtrace.SystemTraceModelAdapter
import com.google.common.collect.ImmutableMap
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.util.Disposer
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * This test verifies that our serialized TPD model used for unit tests and stored in profilers/testData
 * are updated and consistent with what is produced with the real execution of TPD.
 *
 * If you're seeing failures from this test, run `regen TPD model files` test method from inside the IDE and
 * models inside testData will be updated.
 */
class SerializedTraceProcessorModelForTestsVerifier {

  private val fakeFeatureTracker = FakeFeatureTracker()
  private lateinit var service: TraceProcessorServiceImpl

  @Before
  fun setUp() {
    service = TraceProcessorServiceImpl()
  }

  @After
  fun tearDown() {
    // Make sure we dispose the whole service, so we shutdown the daemon.
    Disposer.dispose(service)
  }

  @Test
  fun `test perfetto trace`() {
    val loadOk = service.loadTrace(1, CpuProfilerTestUtils.getTraceFile("perfetto.trace"), fakeFeatureTracker)
    assertThat(loadOk).isTrue()
    val realProcessList = service.getProcessMetadata(1, fakeFeatureTracker)
    val serializedProcesssList = loadSerializedProcessList(CpuProfilerTestUtils.getTraceFile("perfetto.trace_process_list"))
    assertThat(realProcessList).containsExactlyElementsIn(serializedProcesssList).inOrder()

    val sfProcessId = realProcessList.find {
      it.getSafeProcessName().endsWith(SystemTraceSurfaceflingerManager.SURFACEFLINGER_PROCESS_NAME)
    }

    // We load the serialized model map and check that all processes are present.
    val serializedModelMap = loadSerializedModelMap(CpuProfilerTestUtils.getTraceFile("perfetto.trace_tpd_model"))

    for (process in realProcessList) {
      val pid = process.id
      val pidsToQuery = mutableListOf(pid)
      sfProcessId?.let { pidsToQuery.add(it.id) }

      val realModel = service.loadCpuData(1, pidsToQuery, fakeFeatureTracker)
      val serializedModel = serializedModelMap[pid] ?: error("$pid should be present perfetto.trace_tpd_model")
      assertThat(realModel.getCaptureStartTimestampUs()).isEqualTo(serializedModel.getCaptureStartTimestampUs())
      assertThat(realModel.getCaptureEndTimestampUs()).isEqualTo(serializedModel.getCaptureEndTimestampUs())
      assertThat(realModel.getProcesses()).isEqualTo(serializedModel.getProcesses())
      assertThat(realModel.getCpuCores()).isEqualTo(serializedModel.getCpuCores())
    }
  }

  @Test
  fun `test perfetto_cpu_usage trace`() {
    val loadOk = service.loadTrace(1, CpuProfilerTestUtils.getTraceFile("perfetto_cpu_usage.trace"), fakeFeatureTracker)
    assertThat(loadOk).isTrue()
    val realProcessList = service.getProcessMetadata(1, fakeFeatureTracker)
    val serializedProcesssList = loadSerializedProcessList(CpuProfilerTestUtils.getTraceFile("perfetto_cpu_usage.trace_process_list"))
    assertThat(realProcessList).containsExactlyElementsIn(serializedProcesssList).inOrder()

    val sfProcessId = realProcessList.find {
      it.getSafeProcessName().endsWith(SystemTraceSurfaceflingerManager.SURFACEFLINGER_PROCESS_NAME)
    }

    // We load the serialized model map and check that all processes are present.
    val serializedModelMap = loadSerializedModelMap(CpuProfilerTestUtils.getTraceFile("perfetto_cpu_usage.trace_tpd_model"))

    for (process in realProcessList) {
      val pid = process.id
      val pidsToQuery = mutableListOf(pid)
      sfProcessId?.let { pidsToQuery.add(it.id) }

      val realModel = service.loadCpuData(1, pidsToQuery, fakeFeatureTracker)
      val serializedModel = serializedModelMap[pid] ?: error("$pid should be present perfetto_cpu_usage.trace_tpd_model")
      assertThat(realModel.getCaptureStartTimestampUs()).isEqualTo(serializedModel.getCaptureStartTimestampUs())
      assertThat(realModel.getCaptureEndTimestampUs()).isEqualTo(serializedModel.getCaptureEndTimestampUs())
      assertThat(realModel.getProcesses()).isEqualTo(serializedModel.getProcesses())
      assertThat(realModel.getCpuCores()).isEqualTo(serializedModel.getCpuCores())
    }
  }

  private fun loadSerializedProcessList(serializedProcessModelList: File): List<ProcessModel> {
    val ois = ObjectInputStream(FileInputStream(serializedProcessModelList))
    @Suppress("UNCHECKED_CAST")
    val processList = ois.readObject() as List<ProcessModel>
    ois.close()

    return processList
  }

  private fun loadSerializedModelMap(serializedModelMap: File): Map<Int, SystemTraceModelAdapter> {
    val ois = ObjectInputStream(FileInputStream(serializedModelMap))
    @Suppress("UNCHECKED_CAST")
    val modelMap = ois.readObject() as Map<Int, SystemTraceModelAdapter>
    ois.close()

    return modelMap
  }

  @Test
  @Ignore("To be invoked manually to regenerate _process_list and _tpd_model for perfetto traces in testData")
  fun `regen TPD model files`() {
    produceAndWriteModelsFor(CpuProfilerTestUtils.getTraceFile("perfetto.trace"))
    produceAndWriteModelsFor(CpuProfilerTestUtils.getTraceFile("perfetto_cpu_usage.trace"))
  }

  private fun produceAndWriteModelsFor(traceFile: File) {
    val loadOk = service.loadTrace(1, traceFile, fakeFeatureTracker)
    assertThat(loadOk).isTrue()

    val processList = service.getProcessMetadata(1, fakeFeatureTracker)

    val sfProcessId = processList.find {
      it.getSafeProcessName().endsWith(SystemTraceSurfaceflingerManager.SURFACEFLINGER_PROCESS_NAME)
    }

    val modelMapBuilder = ImmutableMap.builder<Int, SystemTraceModelAdapter>()
    for (process in processList) {
      val pid = process.id
      val pidsToQuery = mutableListOf(pid)
      sfProcessId?.let { pidsToQuery.add(it.id) }

      val model = service.loadCpuData(1, pidsToQuery, fakeFeatureTracker)
      modelMapBuilder.put(pid, model)
    }

    val processListModelFile = File(traceFile.parentFile, "${traceFile.name}_process_list")
    writeObjectToFile(processListModelFile, processList)
    val modelMapFile = File(traceFile.parentFile, "${traceFile.name}_tpd_model")
    writeObjectToFile(modelMapFile, modelMapBuilder.build())
  }

  private fun writeObjectToFile(file: File, serializableObject: Any) {
    file.delete()
    file.createNewFile()
    val oos = ObjectOutputStream(FileOutputStream(file))
    oos.writeObject(serializableObject)
    oos.flush()
    oos.close()
  }

}