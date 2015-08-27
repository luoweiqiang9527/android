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
package com.android.tools.idea.editors.gfxtrace;

import com.android.tools.idea.ddms.EdtExecutor;
import com.android.tools.idea.editors.gfxtrace.controllers.*;
import com.android.tools.idea.editors.gfxtrace.service.*;
import com.android.tools.idea.editors.gfxtrace.service.atom.AtomMetadata;
import com.android.tools.idea.editors.gfxtrace.service.path.CapturePath;
import com.android.tools.idea.editors.gfxtrace.service.path.Path;
import com.android.tools.idea.editors.gfxtrace.service.path.PathListener;
import com.android.tools.rpclib.binary.BinaryObject;
import com.android.tools.rpclib.schema.ConstantSet;
import com.android.tools.rpclib.schema.Dynamic;
import com.android.tools.rpclib.schema.SchemaClass;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PluginPathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class GfxTraceEditor extends UserDataHolderBase implements FileEditor {
  @NotNull public static final String SELECT_CAPTURE = "Select a capture";
  @NotNull public static final String SELECT_ATOM = "Select an atom";

  @NotNull private static final String OVERRIDE_PATH_GAPIS = "override.path.gapis";
  @NotNull private static final Logger LOG = Logger.getInstance(GfxTraceEditor.class);
  @NotNull private static final String SERVER_HOST = "localhost";
  @NotNull private static final String SERVER_EXECUTABLE_NAME = "gapis";
  @NotNull private static final String SERVER_RELATIVE_PATH = "bin";
  private static final int SERVER_PORT = 6700;
  private static final int SERVER_LAUNCH_TIMEOUT_MS = 2000;
  private static final int SERVER_LAUNCH_SLEEP_INCREMENT_MS = 10;

  @NotNull private final Project myProject;
  @NotNull private final GfxTraceViewPanel myView;
  @NotNull private final ListeningExecutorService myExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
  private Process myServerProcess;
  private Socket myServerSocket;
  @NotNull private ServiceClient myClient;

  @NotNull private List<PathListener> myPathListeners = new ArrayList<PathListener>();
  private ContextController myContextController;
  private AtomController myAtomController;
  private ScrubberController myScrubberController;
  private FrameBufferController myFrameBufferController;
  private StateController myStateController;
  private DocumentationController myDocumentationController;

  public GfxTraceEditor(@NotNull final Project project, @SuppressWarnings("UnusedParameters") @NotNull final VirtualFile file) {
    myProject = project;
    myView = new GfxTraceViewPanel();

    // Attempt to start/connect to the server on a separate thread to reduce the IDE from stalling.
    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        if (!connectToServer()) {
          setLoadingErrorTextOnEdt("Unable to connect to server");
          return;
        }

        try {
          ServiceClient rpcClient =
            new ServiceClientRPC(myExecutor, myServerSocket.getInputStream(), myServerSocket.getOutputStream(), 1024);
          myClient = new ServiceClientCache(rpcClient);
        }
        catch (IOException e) {
          setLoadingErrorTextOnEdt("Unable to talk to server");
          return;
        }

        // Prefetch the schema
        final ListenableFuture<Schema> schemaF = myClient.getSchema();
        Futures.addCallback(schemaF, new LoadingCallback<Schema>(LOG) {
          @Override
          public void onSuccess(@Nullable final Schema schema) {
            LOG.info("Schema with " + schema.getClasses().length + " classes, " + schema.getConstants().length + " constant sets");
            int atoms = 0;
            for (SchemaClass type : schema.getClasses()) {
              // Find the atom metadata, if present
              if (AtomMetadata.find(type) != null) {
                atoms++;
              }
              Dynamic.register(type);
            }
            LOG.info("Schema with " + atoms + " atoms");
            for (ConstantSet set : schema.getConstants()) {
              ConstantSet.register(set);
            }
          }
        });

        try {
          byte[] data = file.contentsToByteArray();

          // Upload the trace file
          LOG.info("Upload " + data.length + " bytes of gfxtrace as " + file.getPresentableName());
          final ListenableFuture<CapturePath> captureF = myClient.importCapture(file.getPresentableName(), data);

          // When both steps are complete, activate the capture path
          Futures.addCallback(Futures.allAsList(schemaF, captureF), new LoadingCallback<List<BinaryObject>>(LOG) {
            @Override
            public void onSuccess(@Nullable final List<BinaryObject> all) {
              CapturePath path = (CapturePath)all.get(1);
              LOG.info("Capture uploaded");
              if (path != null) {
                activatePath(path);
              }
              else {
                LOG.error("Invalid capture file " + file.getPresentableName());
              }
            }
          });
        }
        catch (IOException e) {
          setLoadingErrorTextOnEdt("Error reading gfxtrace file");
          return;
        }

        myContextController = new ContextController(GfxTraceEditor.this);
        myAtomController = new AtomController(GfxTraceEditor.this, project, myView.getAtomScrollPane());
        myScrubberController = new ScrubberController(GfxTraceEditor.this, myView.getScrubberScrollPane(), myView.getScrubberList());
        myFrameBufferController =
          new FrameBufferController(GfxTraceEditor.this, myView.getColorScrollPane(), myView.getWireframeScrollPane(),
                                    myView.getDepthScrollPane());
        myStateController = new StateController(GfxTraceEditor.this, myView.getStateScrollPane());
        // TODO: Rewrite to use IntelliJ documentation view.
        myDocumentationController = new DocumentationController(GfxTraceEditor.this, myView.getDocsTextPane());

        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            myView.finalizeUi(myProject, myContextController.getContextAction());
          }
        });
      }
    });
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myView.getRootComponent();
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return null;
  }

  @NotNull
  @Override
  public String getName() {
    return "GfxTraceView";
  }

  public void activatePath(@NotNull final Path path) {
    // All path notifications are executed in the editor thread
    EdtExecutor.INSTANCE.execute(new Runnable() {
      @Override
      public void run() {
        LOG.warn("Activate path " + path);
        for (PathListener listener : myPathListeners) {
          listener.notifyPath(path);
        }
      }
    });
  }

  public void addPathListener(@NotNull PathListener listener) {
    myPathListeners.add(listener);
  }

  public void notifyDimensionChanged(@NotNull Dimension newDimension) {
    myView.resize();
  }

  @NotNull
  @Override
  public FileEditorState getState(@NotNull FileEditorStateLevel level) {
    return FileEditorState.INSTANCE;
  }

  @Override
  public void setState(@NotNull FileEditorState state) {

  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void selectNotify() {

  }

  @Override
  public void deselectNotify() {

  }

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

  }

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

  }

  @Nullable
  @Override
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    return null;
  }

  @Nullable
  @Override
  public FileEditorLocation getCurrentLocation() {
    return null;
  }

  @Nullable
  @Override
  public StructureViewBuilder getStructureViewBuilder() {
    return null;
  }

  @NotNull
  public ServiceClient getClient() {
    return myClient;
  }

  @NotNull
  public ListeningExecutorService getExecutor() {
    return myExecutor;
  }

  @Override
  public void dispose() {
    shutdown();
  }

  /**
   * Attempts to connect to a gapis server.
   * <p/>
   * If the first attempt to connect fails, will launch a new server process and attempt to connect again.
   * <p/>
   * TODO: Implement more robust process management.  For example:
   * TODO: - Better handling of shutdown so that the replayd process does not continue running.
   * TODO: - Better way to detect when server has started in order to avoid polling for the socket.
   *
   * @return true if a connection to the server was established.
   */
  private boolean connectToServer() {
    assert !ApplicationManager.getApplication().isDispatchThread();

    Factory.register();

    myServerSocket = null;
    try {
      // Try to connect to an existing server.
      myServerSocket = new Socket(SERVER_HOST, SERVER_PORT);
    }
    catch (IOException ignored) {
    }

    if (myServerSocket != null) {
      return true;
    }

    // The connection failed, so try to start a new instance of the server.
    try {
      File androidPluginDirectory = PluginPathManager.getPluginHome("android");
      String pathOverride = System.getProperty(OVERRIDE_PATH_GAPIS);
      if (pathOverride.length() > 0) {
        androidPluginDirectory = new File(pathOverride);
      }
      File serverDirectory = new File(androidPluginDirectory, SERVER_RELATIVE_PATH);
      File serverExecutable = new File(serverDirectory, SERVER_EXECUTABLE_NAME);
      LOG.info("launch gapis: \"" + serverExecutable.getAbsolutePath() + "\"");
      ProcessBuilder pb = new ProcessBuilder(serverExecutable.getAbsolutePath());

      // Add the server's directory to the path.  This allows the server to find and launch the replayd.
      Map<String, String> env = pb.environment();
      String path = env.get("PATH");
      path = serverDirectory.getAbsolutePath() + File.pathSeparator + path;
      env.put("PATH", path);

      // Use the plugin directory as the working directory for the server.
      pb.directory(androidPluginDirectory);

      // This will throw IOException if the server executable is not found.
      myServerProcess = pb.start();
    }
    catch (IOException e) {
      LOG.warn(e);
      return false;
    }

    // After starting, the server requires a little time before it will be ready to accept connections.
    // This loop polls the server to establish a connection.
    for (int waitTime = 0; waitTime < SERVER_LAUNCH_TIMEOUT_MS; waitTime += SERVER_LAUNCH_SLEEP_INCREMENT_MS) {
      try {
        myServerSocket = new Socket(SERVER_HOST, SERVER_PORT);
        return true;
      }
      catch (IOException e1) {
        myServerSocket = null;
      }

      try {
        // Wait before trying again.
        Thread.sleep(SERVER_LAUNCH_SLEEP_INCREMENT_MS);
      }
      catch (InterruptedException e) {
        Thread.interrupted(); // reset interrupted status
        shutdown();
        return false; // Some external factor cancelled our busy-wait, so exit immediately.
      }
    }

    shutdown();
    return false;
  }

  private void setLoadingErrorTextOnEdt(@NotNull final String error) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        myView.setLoadingError(error);
      }
    });
  }

  private void shutdown() {
    if (myServerSocket != null) {
      try {
        myServerSocket.close();
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }

    // Only kill the server if we started it.
    if (myServerProcess != null) {
      myServerProcess.destroy();
    }

    myExecutor.shutdown();
  }
}
