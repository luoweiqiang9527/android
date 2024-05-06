/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.appinspection.internal

import com.android.tools.app.inspection.AppInspection
import com.android.tools.idea.appinspection.inspector.api.process.ProcessDescriptor
import com.android.tools.idea.transport.TransportClient
import com.android.tools.idea.transport.manager.StreamEvent
import com.android.tools.idea.transport.manager.StreamEventQuery
import com.android.tools.idea.transport.manager.TransportStreamChannel
import com.android.tools.profiler.proto.Commands
import com.android.tools.profiler.proto.Common
import com.android.tools.profiler.proto.Transport
import com.google.common.annotations.VisibleForTesting
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

fun Commands.Command.toExecuteRequest(): Transport.ExecuteRequest =
  Transport.ExecuteRequest.newBuilder().setCommand(this).build()

/** Small helper class to work with the one exact process and app-inspection events & commands. */
class AppInspectionTransport(
  val client: TransportClient,
  val process: ProcessDescriptor,
  private val streamChannel: TransportStreamChannel
) {

  companion object {
    private val commandIdGenerator = AtomicInteger(1)

    /**
     * A method which generates a new unique ID each time, to be assigned to an outgoing inspector
     * command.
     *
     * This ID is used to map events from the agent to the correct handler. This method is
     * thread-safe.
     */
    fun generateNextCommandId() = commandIdGenerator.getAndIncrement()

    /**
     * The last value generated by calling [generateNextCommandId]
     *
     * This method is thread-safe.
     */
    @VisibleForTesting fun lastGeneratedCommandId() = commandIdGenerator.get() - 1
  }

  /** Utility function to create a [StreamEventQuery] based on this pid. */
  fun createStreamEventQuery(
    eventKind: Common.Event.Kind,
    filter: (Common.Event) -> Boolean = { true },
    startTimeNs: () -> Long = { Long.MIN_VALUE }
  ) =
    StreamEventQuery(
      eventKind = eventKind,
      startTime = startTimeNs,
      filter = filter,
      processId = { process.pid }
    )

  /** Creates a flow that subscribes to events filtered by the provided filtering criteria. */
  fun eventFlow(
    eventKind: Common.Event.Kind,
    filter: (Common.Event) -> Boolean = { true },
    startTimeNs: () -> Long = { Long.MIN_VALUE }
  ): Flow<StreamEvent> {
    val query = createStreamEventQuery(eventKind, filter, startTimeNs)
    return streamChannel.eventFlow(query)
  }

  private fun AppInspection.AppInspectionCommand.toCommand() =
    Commands.Command.newBuilder()
      .setType(Commands.Command.CommandType.APP_INSPECTION)
      .setStreamId(process.streamId)
      .setPid(process.pid)
      .setAppInspectionCommand(this)
      .build()

  /**
   * Identical in functionality to [executeCommand] below except it takes an AppInspection [command]
   * .
   */
  suspend fun executeCommand(
    command: AppInspection.AppInspectionCommand,
    streamEventQuery: StreamEventQuery
  ): Common.Event {
    return executeCommand(command.toCommand().toExecuteRequest(), streamEventQuery)
  }

  /**
   * Executes the provided [command] and await for a response that satisfies the provided
   * [streamEventQuery].
   */
  suspend fun executeCommand(
    request: Transport.ExecuteRequest,
    streamEventQuery: StreamEventQuery
  ): Common.Event {
    executeCommand(request)
    return streamChannel.eventFlow(streamEventQuery).first().event
  }

  /** Sends an app inspection command via the transport pipeline to device. */
  fun executeCommand(appInspectionCommand: AppInspection.AppInspectionCommand) {
    val command =
      Commands.Command.newBuilder()
        .setType(Commands.Command.CommandType.APP_INSPECTION)
        .setStreamId(process.streamId)
        .setPid(process.pid)
        .setAppInspectionCommand(appInspectionCommand)
        .build()
    executeCommand(command.toExecuteRequest())
  }

  /**
   * Executes a command. This call blocks the thread for a little bit while waiting for a response.
   */
  private fun executeCommand(request: Transport.ExecuteRequest) {
    //noinspection CheckResult
    client.transportStub.execute(request)
  }
}
