/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.run.tasks;

import static com.intellij.execution.process.ProcessOutputTypes.STDERR;

import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.logcat.LogCatMessage;
import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.logcat.AndroidLogcatFormatter;
import com.android.tools.idea.logcat.AndroidLogcatPreferences;
import com.android.tools.idea.logcat.AndroidLogcatService;
import com.android.tools.idea.logcat.AndroidLogcatService.LogcatListener;
import com.android.tools.idea.logcat.LogcatHeaderFormat;
import com.android.tools.idea.logcat.LogcatHeaderFormat.TimestampFormat;
import com.android.tools.idea.logcat.output.LogcatOutputConfigurableProvider;
import com.android.tools.idea.logcat.output.LogcatOutputSettings;
import com.android.tools.idea.run.AndroidDebugState;
import com.android.tools.idea.run.AndroidProcessText;
import com.android.tools.idea.run.AndroidRemoteDebugProcessHandler;
import com.android.tools.idea.run.AndroidSessionInfo;
import com.android.tools.idea.run.ApplicationIdProvider;
import com.android.tools.idea.run.ApplicationLogListener;
import com.android.tools.idea.run.LaunchInfo;
import com.android.tools.idea.run.ProcessHandlerConsolePrinter;
import com.android.tools.idea.run.debug.StartJavaDebuggerKt;
import com.android.tools.idea.run.util.ProcessHandlerLaunchStatus;
import com.android.tools.idea.testartifacts.instrumented.testsuite.api.AndroidTestSuiteConstantsKt;
import com.google.common.base.Preconditions;
import com.intellij.debugger.ui.DebuggerPanelsManager;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import java.time.ZoneId;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;

public class ConnectJavaDebuggerTask extends ConnectDebuggerTaskBase {

  public ConnectJavaDebuggerTask(@NotNull ApplicationIdProvider applicationIdProvider, //Set<String> applicationIds,
                                 @NotNull Project project,
                                 boolean attachToRunningProcess) {
    super(applicationIdProvider, project, attachToRunningProcess);
  }

  @Override
  public ProcessHandler launchDebugger(@NotNull LaunchInfo currentLaunchInfo,
                                       @NotNull final Client client,
                                       @NotNull ProcessHandlerLaunchStatus launchStatus,
                                       @NotNull ProcessHandlerConsolePrinter printer) {
    Logger logger = Logger.getInstance(ConnectJavaDebuggerTask.class);

    ProcessHandler processHandler = launchStatus.getProcessHandler();
    // Reuse the current ConsoleView to retain the UI state and not to lose test results.
    Object androidTestResultListener = processHandler.getCopyableUserData(AndroidTestSuiteConstantsKt.ANDROID_TEST_RESULT_LISTENER_KEY);

    if (StudioFlags.NEW_EXECUTION_FLOW_FOR_JAVA_DEBUGGER.get()) {
      logger.info("Attaching Java debugger");
      StartJavaDebuggerKt.attachJavaDebuggerToClient(
        myProject,
        client,
        currentLaunchInfo.env,
        (ConsoleView)androidTestResultListener,
        () -> {
          processHandler.detachProcess();
          return null;
        },
        null
      ).onSuccess(XDebugSessionImpl::showSessionTab);
      return null;
    }

    String debugPort = Integer.toString(client.getDebuggerListenPort());
    final int pid = client.getClientData().getPid();
    logger.info(String.format(Locale.US, "Attempting to connect debugger to port %1$s [client %2$d]", debugPort, pid));

    RunContentDescriptor descriptor = Preconditions.checkNotNull(processHandler.getUserData(AndroidSessionInfo.KEY)).getDescriptor();

    // create a new process handler
    RemoteConnection connection = new RemoteConnection(true, "localhost", debugPort, false);
    ProcessHandler debugProcessHandler = new AndroidRemoteDebugProcessHandler(myProject, client, false);

    // switch the launch status and console printers to point to the new process handler
    // this is required, esp. for AndroidTestListener which holds a reference to the launch status and printers, and those should
    // be updated to point to the new process handlers, otherwise test results will not be forwarded appropriately
    launchStatus.setProcessHandler(debugProcessHandler);
    printer.setProcessHandler(debugProcessHandler);

    // detach after the launch status has been updated to point to the new process handler
    processHandler.detachProcess();

    final AndroidDebugState debugState;

    if (androidTestResultListener instanceof ConsoleView) {
      ConsoleView consoleViewToReuse = (ConsoleView)androidTestResultListener;
      debugState = new AndroidDebugState(myProject, debugProcessHandler, connection, (parent, handler, executor) -> {
        consoleViewToReuse.attachToProcess(handler);
        return consoleViewToReuse;
      });
    }
    else {
      debugState = new AndroidDebugState(myProject, debugProcessHandler, connection, currentLaunchInfo.consoleProvider);
    }

    RunContentDescriptor debugDescriptor;
    try {
      // @formatter:off
      ExecutionEnvironment debugEnv = new ExecutionEnvironmentBuilder(currentLaunchInfo.env)
        .executor(currentLaunchInfo.executor)
        .runner(currentLaunchInfo.runner)
        .contentToReuse(descriptor)
        .build();
      debugDescriptor = DebuggerPanelsManager.getInstance(myProject).attachVirtualMachine(debugEnv, debugState, connection, false);
      // @formatter:on
    }
    catch (ExecutionException e) {
      processHandler.notifyTextAvailable("ExecutionException: " + e.getMessage() + '.', STDERR);
      return null;
    }

    if (debugDescriptor == null) {
      processHandler.notifyTextAvailable("Unable to connect debugger to Android application", STDERR);
      return null;
    }

    // re-run the collected text from the old process handler to the new
    // TODO: is there a race between messages received once the debugger has been connected, and these messages that are printed out?
    final AndroidProcessText oldText = AndroidProcessText.get(processHandler);
    if (oldText != null) {
      oldText.printTo(debugProcessHandler);
    }

    RunProfile runProfile = currentLaunchInfo.env.getRunProfile();
    RunConfiguration runConfiguration = runProfile instanceof RunConfiguration ? (RunConfiguration)runProfile : null;
    AndroidSessionInfo.create(debugProcessHandler, debugDescriptor, runConfiguration, currentLaunchInfo.executor.getId(),
                              currentLaunchInfo.executor.getActionName(),
                              currentLaunchInfo.env.getExecutionTarget()
    );
    debugProcessHandler.putUserData(AndroidSessionInfo.ANDROID_DEBUG_CLIENT, client);
    debugProcessHandler.putUserData(AndroidSessionInfo.ANDROID_DEVICE_API_LEVEL, client.getDevice().getVersion());

    captureLogcatOutput(client, debugProcessHandler);

    return debugProcessHandler;
  }

  private static void captureLogcatOutput(@NotNull Client client,
                                          @NotNull ProcessHandler debugProcessHandler) {
    if (!StudioFlags.RUNDEBUG_LOGCAT_CONSOLE_OUTPUT_ENABLED.get()) {
      return;
    }
    if (!LogcatOutputSettings.getInstance().isDebugOutputEnabled()) {
      return;
    }

    final IDevice device = client.getDevice();
    LogcatListener logListener = new MyLogcatListener(client, debugProcessHandler);

    Logger.getInstance(ConnectJavaDebuggerTask.class).info(String.format("captureLogcatOutput(\"%s\")", device.getName()));
    AndroidLogcatService.getInstance().addListener(device, logListener, true);

    // Remove listener when process is terminated
    debugProcessHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        Logger.getInstance(ConnectJavaDebuggerTask.class)
          .info(String.format("captureLogcatOutput(\"%s\"): remove listener", device.getName()));
        AndroidLogcatService.getInstance().removeListener(device, logListener);
      }
    });
  }

  private static final class MyLogcatListener extends ApplicationLogListener {
    private static final LogcatHeaderFormat SIMPLE_FORMAT = new LogcatHeaderFormat(TimestampFormat.NONE, false, false, true);

    private final AndroidLogcatFormatter myFormatter;
    private final AtomicBoolean myIsFirstMessage;
    private final ProcessHandler myDebugProcessHandler;

    private MyLogcatListener(@NotNull Client client, @NotNull ProcessHandler debugProcessHandler) {
      // noinspection ConstantConditions
      super(client.getClientData().getClientDescription(), client.getClientData().getPid());

      myFormatter = new AndroidLogcatFormatter(ZoneId.systemDefault(), new AndroidLogcatPreferences());
      myIsFirstMessage = new AtomicBoolean(true);
      myDebugProcessHandler = debugProcessHandler;
    }

    @NotNull
    @Override
    protected String formatLogLine(@NotNull LogCatMessage line) {
      return myFormatter.formatMessage(SIMPLE_FORMAT, line.getHeader(), line.getMessage());
    }

    @Override
    protected void notifyTextAvailable(@NotNull String message, @NotNull Key key) {
      if (myIsFirstMessage.compareAndSet(true, false)) {
        myDebugProcessHandler.notifyTextAvailable(LogcatOutputConfigurableProvider.BANNER_MESSAGE + '\n', ProcessOutputTypes.STDOUT);
      }

      myDebugProcessHandler.notifyTextAvailable(message, key);
    }
  }
}
