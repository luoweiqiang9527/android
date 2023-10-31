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
package com.android.tools.idea.layoutinspector.pipeline.adb

import com.android.fakeadbserver.DeviceState
import com.android.fakeadbserver.FakeAdbServer
import com.android.fakeadbserver.devicecommandhandlers.DeviceCommandHandler
import kotlinx.coroutines.CoroutineScope
import java.net.Socket
import java.util.ArrayDeque
import java.util.Deque

class SimpleCommand(val args: List<String>, val result: String?) {
  constructor(command: String, result: String?) : this(command.split(' '), result)
}

/**
 * A fake handler that intercepts ADB shell commands used at various points by the layout inspector.
 */
class FakeShellCommandHandler : DeviceCommandHandler("shell"), AdbDebugViewProperties {
  override var debugViewAttributes: String? = null
  override var debugViewAttributesApplicationPackage: String? = null
  override var debugViewAttributesChangesCount: Int = 0
  val extraCommands = mutableListOf<SimpleCommand>()

  override fun accept(
    server: FakeAdbServer,
    socketScope: CoroutineScope,
    socket: Socket,
    device: DeviceState,
    command: String,
    args: String
  ): Boolean {
    val response =
      when (command) {
        "shell" -> handleShellCommand(args) ?: return false
        else -> return false
      }
    writeOkay(socket.getOutputStream())
    writeString(socket.getOutputStream(), response)
    return true
  }

  private fun handleShellCommand(argsAsString: String): String? {
    val args = argsAsString.split(' ')
    // DebugViewAttributes spawns a blocking subshell on a background thread in production; this
    // flow is not easily testable so just
    // treat it like a no-op.
    return when (args.firstOrNull()) {
      "sh" -> ""
      "echo" -> args.subList(1, args.size).joinToString(" ")
      "settings" -> handleSettingsCommand(ArrayDeque(args.subList(1, args.size)))
      else -> extraCommands.find { it.args == args }?.result
    }
  }

  private fun handleSettingsCommand(args: Deque<String>): String? {
    val operation = args.poll()
    if (args.poll() != "global") {
      return null
    }
    val variable =
      when (args.poll()) {
        "debug_view_attributes" -> this::debugViewAttributes
        "debug_view_attributes_application_package" -> this::debugViewAttributesApplicationPackage
        else -> return null
      }
    val argument = if (args.isEmpty()) "" else args.poll()
    if (args.isNotEmpty()) {
      return null
    }
    return when (operation) {
      "get" -> {
        variable.get().toString()
      }
      "put" -> {
        variable.set(argument)
        debugViewAttributesChangesCount++
        ""
      }
      "delete" -> {
        variable.set(null)
        debugViewAttributesChangesCount++
        ""
      }
      else -> null
    }
  }
}
