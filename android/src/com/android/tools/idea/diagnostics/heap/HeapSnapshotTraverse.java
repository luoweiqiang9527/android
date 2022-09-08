/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.tools.idea.diagnostics.heap;

import static com.android.tools.idea.diagnostics.heap.HeapTraverseUtil.processMask;
import static com.android.tools.idea.util.StudioPathManager.isRunningFromSources;
import static com.google.common.math.IntMath.isPowerOfTwo;
import static com.google.wireless.android.sdk.stats.MemoryUsageReportEvent.MemoryUsageCollectionMetadata.StatusCode;

import com.android.tools.analytics.UsageTracker;
import com.android.tools.idea.util.StudioPathManager;
import com.google.wireless.android.sdk.stats.AndroidStudioEvent;
import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.WeakList;
import com.intellij.util.system.CpuArch;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HeapSnapshotTraverse {

  private static final int MAX_ALLOWED_OBJECT_MAP_SIZE = 1_000_000;
  private static final int INVALID_OBJECT_ID = -1;
  private static final int MAX_DEPTH = 100_000;

  private static final long OBJECT_CREATION_ITERATION_ID_MASK = 0xFF;
  private static final long CURRENT_ITERATION_ID_MASK = 0xFF00;
  private static final long CURRENT_ITERATION_VISITED_MASK = 0x10000;
  private static final long CURRENT_ITERATION_OBJECT_ID_MASK = 0x1FFFFFFFE0000L;

  private static final int CURRENT_ITERATION_OBJECT_ID_OFFSET = 17;
  // 8(creation iteration id mask) + 8(current iteration id mask) + 1(visited mask)
  private static final int CURRENT_ITERATION_ID_OFFSET = 8;

  private static final String DIAGNOSTICS_HEAP_NATIVE_PATH =
    "tools/adt/idea/android/src/com/android/tools/idea/diagnostics/heap/native";
  private static final String JNI_OBJECT_TAGGER_LIB_NAME = "jni_object_tagger";
  private static final String RESOURCES_NATIVE_PATH = "plugins/android/resources/native";
  private static final Logger LOG = Logger.getInstance(HeapSnapshotTraverse.class);
  static boolean ourAgentWasSuccessfullyLoaded = false;
  private static short ourIterationId = 0;

  private volatile boolean myShouldAbortTraversal = false;

  static {
    try {
      loadObjectTaggingAgent();
      ourAgentWasSuccessfullyLoaded = true;
    }
    catch (HeapSnapshotTraverseException | IOException e) {
      LOG.warn("Native object tagging library is not available", e);
    }
  }

  @NotNull private final LowMemoryWatcher myWatcher;
  @NotNull private final HeapTraverseChildProcessor myHeapTraverseChildProcessor;
  private final short myIterationId;
  @NotNull private final HeapSnapshotStatistics myStatistics;
  private int myLastObjectId = 0;

  public HeapSnapshotTraverse(@NotNull final HeapSnapshotStatistics statistics) {
    this(new HeapTraverseChildProcessor(statistics), statistics);
  }

  public HeapSnapshotTraverse(@NotNull final HeapTraverseChildProcessor childProcessor, @NotNull final HeapSnapshotStatistics statistics) {
    myWatcher = LowMemoryWatcher.register(this::onLowMemorySignalReceived);
    myHeapTraverseChildProcessor = childProcessor;
    myIterationId = getNextIterationId();
    myStatistics = statistics;
  }

  /**
   * The heap traversal algorithm is the following:
   * <p>
   * In the process of traversal, we associate a number of masks with each object. These masks are stored in {@link HeapTraverseNode} and
   * show which components own the corresponding object(myOwnedByComponentMask), which components retain the object(myRetainedMask) etc.
   * <p>
   * On the first pass along the heap we arrange objects in topological order (in terms of references). This is necessary so that during the
   * subsequent propagation of masks, we can be sure that all objects that refer to the object have already been processed and masks were
   * updated.
   * <p>
   * On the second pass, we directly update the masks and pass them to the referring objects.
   *
   * @param maxDepth   the maximum depth to which we will descend when traversing the object tree.
   * @param startRoots objects from which traversal is started.
   */
  public StatusCode walkObjects(int maxDepth, @NotNull final Collection<?> startRoots) {
    try {
      if (!canTagObjects()) {
        return StatusCode.CANT_TAG_OBJECTS;
      }
      final FieldCache fieldCache = new FieldCache(myStatistics);

      // enumerating heap objects in topological order
      for (Object root : startRoots) {
        if (root == null) continue;
        depthFirstTraverseHeapObjects(root, maxDepth, fieldCache);
      }
      // By this moment all the reachable heap objects are enumerated in topological order and marked as visited.
      // Order id, visited and the iteration id are stored in objects tags.
      // We also use this enumeration to kind of "freeze" the state of the heap, and we will ignore all the newly allocated object
      // that were allocated after the enumeration pass.
      final Map<Integer, HeapTraverseNode> objectIdToTraverseNode = new Int2ObjectOpenHashMap<>();

      for (Object root : startRoots) {
        int objectId = getObjectId(root);
        if (objectId <= 0 || objectId > myLastObjectId) {
          return StatusCode.WRONG_ROOT_OBJECT_ID;
        }
        objectIdToTraverseNode.put(objectId, new HeapTraverseNode(root));
      }

      myStatistics.setHeapObjectCount(myLastObjectId);
      myStatistics.setTraverseSessionId(myIterationId);

      // iterate over objects in topological order and update masks
      for (int i = myLastObjectId; i > 0; i--) {
        abortTraversalIfRequested();
        myStatistics.updateMaxObjectsQueueSize(objectIdToTraverseNode.size());
        if (objectIdToTraverseNode.size() > MAX_ALLOWED_OBJECT_MAP_SIZE) {
          return StatusCode.OBJECTS_MAP_IS_TOO_BIG;
        }
        HeapTraverseNode node = objectIdToTraverseNode.get(i);

        if (node == null) {
          myStatistics.incrementGarbageCollectedObjectsCounter();
          continue;
        }
        objectIdToTraverseNode.remove(i);

        final Object currentObject = node.getObject();
        if (currentObject == null) {
          myStatistics.incrementGarbageCollectedObjectsCounter();
          continue;
        }

        // Check whether the current object is a root of one of the components
        ComponentsSet.Component currentObjectComponent = myStatistics.getComponentsSet().getComponentOfObject(currentObject);
        long currentObjectSize = getObjectSize(currentObject);
        short currentObjectCreationIterationId = getObjectCreationIterationId(currentObject);
        short currentObjectAge = (short)(myIterationId - currentObjectCreationIterationId);

        myStatistics.addObjectToTotal(currentObjectSize, currentObjectAge);

        // if it's a root of a component
        if (currentObjectComponent != null) {
          updateComponentRootMasks(node, currentObjectComponent, HeapTraverseNode.RefWeight.DEFAULT);
        }

        // If current object is retained by any components - propagate their stats.
        processMask(node.myRetainedMask,
                    (index) -> myStatistics.addRetainedObjectSizeToComponent(index, currentObjectSize, currentObjectAge));
        // If current object is retained by any component categories - propagate their stats.
        processMask(node.myRetainedMaskForCategories,
                    (index) -> myStatistics.addRetainedObjectSizeToCategoryComponent(index, currentObjectSize, currentObjectAge));

        AtomicInteger categoricalOwnedMask = new AtomicInteger();
        processMask(node.myOwnedByComponentMask,
                    (index) -> categoricalOwnedMask.set(
                      categoricalOwnedMask.get() |
                      1 << myStatistics.getComponentsSet().getComponents().get(index).getComponentCategory().getId()));
        if (categoricalOwnedMask.get() != 0 && isPowerOfTwo(categoricalOwnedMask.get())) {
          processMask(categoricalOwnedMask.get(),
                      (index) -> myStatistics.addOwnedObjectSizeToCategoryComponent(index, currentObjectSize, currentObjectAge));
        }
        if (node.myOwnedByComponentMask == 0) {
          int uncategorizedComponentId = myStatistics.getComponentsSet().getUncategorizedComponent().getId();
          int uncategorizedCategoryId = myStatistics.getComponentsSet().getUncategorizedComponent().getComponentCategory().getId();
          myStatistics.addOwnedObjectSizeToComponent(uncategorizedComponentId, currentObjectSize, currentObjectAge);
          myStatistics.addOwnedObjectSizeToCategoryComponent(uncategorizedCategoryId, currentObjectSize, currentObjectAge);
        }
        else if (isPowerOfTwo(node.myOwnedByComponentMask)) {
          // if only owned by one component
          processMask(node.myOwnedByComponentMask,
                      (index) -> myStatistics.addOwnedObjectSizeToComponent(index, currentObjectSize, currentObjectAge));
        }
        else {
          // if owned by multiple components -> add to shared
          myStatistics.addObjectSizeToSharedComponent(node.myOwnedByComponentMask, currentObjectSize, currentObjectAge);
        }

        // propagate to referred objects
        propagateComponentMask(currentObject, node, objectIdToTraverseNode, fieldCache);
      }
    }
    catch (HeapSnapshotTraverseException exception) {
      return exception.getStatusCode();
    }
    finally {
      myWatcher.stop();
    }
    return StatusCode.NO_ERROR;
  }

  private void updateComponentRootMasks(HeapTraverseNode node,
                                        ComponentsSet.Component currentObjectComponent,
                                        HeapTraverseNode.RefWeight weight) {
    node.myRetainedMask |= (1 << currentObjectComponent.getId());
    node.myRetainedMaskForCategories |= (1 << currentObjectComponent.getComponentCategory().getId());
    node.myOwnedByComponentMask = (1 << currentObjectComponent.getId());
    node.myOwnershipWeight = weight;
  }

  private void abortTraversalIfRequested() throws HeapSnapshotTraverseException {
    if (myShouldAbortTraversal) {
      throw new HeapSnapshotTraverseException(StatusCode.LOW_MEMORY);
    }
  }

  private void onLowMemorySignalReceived() {
    myShouldAbortTraversal = true;
  }

  /**
   * Checks that the passed tag was set during the current traverse.
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean isTagFromTheCurrentIteration(long tag) {
    return ((tag & CURRENT_ITERATION_ID_MASK) >> CURRENT_ITERATION_ID_OFFSET) == myIterationId;
  }

  private short getObjectCreationIterationId(@NotNull final Object obj) {
    long tag = getObjectTag(obj);
    return (short)(tag & OBJECT_CREATION_ITERATION_ID_MASK);
  }

  private void checkObjectCreationIterationIdAndSetIfNot(@NotNull final Object obj) {
    long tag = getObjectTag(obj);
    int creationIterationId = (int)(tag & OBJECT_CREATION_ITERATION_ID_MASK);
    if (creationIterationId == 0) {
      tag &= ~myIterationId;
      tag |= myIterationId;
      setObjectTag(obj, tag | myIterationId);
    }
  }

  private int getObjectId(@NotNull final Object obj) {
    long tag = getObjectTag(obj);

    if (!isTagFromTheCurrentIteration(tag)) {
      return INVALID_OBJECT_ID;
    }
    return (int)(tag >> CURRENT_ITERATION_OBJECT_ID_OFFSET);
  }

  private boolean wasVisited(@NotNull final Object obj) {
    long tag = getObjectTag(obj);
    if (!isTagFromTheCurrentIteration(tag)) {
      return false;
    }
    return (tag & CURRENT_ITERATION_VISITED_MASK) != 0;
  }

  private void setObjectId(@NotNull final Object obj, int newObjectId) {
    long tag = getObjectTag(obj);
    tag &= ~CURRENT_ITERATION_OBJECT_ID_MASK;
    tag |= (long)newObjectId << CURRENT_ITERATION_OBJECT_ID_OFFSET;
    tag &= ~CURRENT_ITERATION_ID_MASK;
    tag |= (long)myIterationId << CURRENT_ITERATION_ID_OFFSET;
    setObjectTag(obj, tag);
  }

  private void markVisited(@NotNull final Object obj) {
    long tag = getObjectTag(obj);
    tag &= ~CURRENT_ITERATION_VISITED_MASK;
    tag |= CURRENT_ITERATION_VISITED_MASK;
    tag &= ~CURRENT_ITERATION_ID_MASK;
    tag |= (long)myIterationId << CURRENT_ITERATION_ID_OFFSET;
    setObjectTag(obj, tag);
  }

  private void addToStack(@NotNull final Node node, int maxDepth, @Nullable final Object value, @NotNull final Deque<Node> stack) {
    if (value == null) {
      return;
    }
    if (node.getDepth() + 1 > maxDepth) {
      return;
    }
    if (HeapTraverseUtil.isPrimitive(value.getClass())) {
      return;
    }
    if (wasVisited(value)) {
      return;
    }

    markVisited(value);
    stack.push(new Node(value, node.getDepth() + 1));
  }

  private void addStronglyReferencedChildrenToStack(@NotNull final Node node,
                                                    int maxDepth,
                                                    @NotNull final Deque<Node> stack,
                                                    @NotNull final FieldCache fieldCache) throws HeapSnapshotTraverseException {
    if (node.myDepth >= maxDepth) {
      return;
    }
    myHeapTraverseChildProcessor.processChildObjects(node.getObject(),
                                                     (Object value, HeapTraverseNode.RefWeight weight) -> addToStack(node, maxDepth, value,
                                                                                                                     stack), fieldCache);
  }

  private int getNextObjectId() {
    return ++myLastObjectId;
  }

    /*
    Object tags have the following structure (in right-most bit order):
    8bits - object creation iteration id
    8bits - current iteration id (used for validation of below fields)
    1bit - visited
    32bits - topological order id
   */

  private void depthFirstTraverseHeapObjects(@NotNull final Object root, int maxDepth, @NotNull final FieldCache fieldCache)
    throws HeapSnapshotTraverseException {
    if (wasVisited(root)) {
      return;
    }
    Deque<Node> stack = new ArrayDeque<>(1_000_000);
    Node rootNode = new Node(root, 0);
    markVisited(root);
    stack.push(rootNode);

    // DFS starting from the given root object.
    while (!stack.isEmpty()) {
      Node node = stack.peek();
      Object obj = node.getObject();
      if (obj == null) {
        stack.pop();
        continue;
      }
      // add to the topological order when ascending from the recursive subtree.
      if (node.myReferencesProcessed) {
        if (node.getObject() != null) {
          checkObjectCreationIterationIdAndSetIfNot(obj);
          setObjectId(node.getObject(), getNextObjectId());
        }
        stack.pop();
        continue;
      }

      addStronglyReferencedChildrenToStack(node, maxDepth, stack, fieldCache);
      abortTraversalIfRequested();
      node.myReferencesProcessed = true;
    }
  }

  /**
   * Distributing object masks to referring objects.
   * <p>
   * Masks contain information about object ownership and retention.
   * <p>
   * By objects owned by a component CompA we mean objects that are reachable from one of the roots of the CompA and not directly
   * reachable from roots of other components (only through CompA root).
   * <p>
   * By component retained objects we mean objects that are only reachable through one of the component roots. Component retained objects
   * for the component also contains objects owned by other components but all of them will be unreachable from GC roots after removing the
   * component roots, so retained objects can be considered as an "additional weight" of the component.
   * <p>
   * We also added weights to object references in order to separate difference types of references and handle situations of shared
   * ownership. Reference types listed in {@link HeapTraverseNode.RefWeight}.
   *
   * @param parentObj              processing object
   * @param parentNode             contains object-specific information (masks)
   * @param objectIdToTraverseNode mapping from object id to corresponding {@link HeapTraverseNode}
   * @param fieldCache             cache that stores fields declared for the given class.
   */
  private void propagateComponentMask(@NotNull final Object parentObj,
                                      @NotNull final HeapTraverseNode parentNode,
                                      final Map<Integer, HeapTraverseNode> objectIdToTraverseNode,
                                      @NotNull final FieldCache fieldCache) throws HeapSnapshotTraverseException {
    myHeapTraverseChildProcessor.processChildObjects(parentObj, (Object value, HeapTraverseNode.RefWeight ownershipWeight) -> {
      if (value == null) {
        return;
      }
      int objectId = getObjectId(value);
      // don't process non-enumerated objects.
      // This situation may occur if array/list element or field value changed after enumeration traversal. We don't process them
      // because they can break the topological ordering.
      if (objectId == INVALID_OBJECT_ID) {
        return;
      }
      if (parentObj.getClass().isSynthetic()) {
        ownershipWeight = HeapTraverseNode.RefWeight.SYNTHETIC;
      }
      if (parentNode.myOwnedByComponentMask == 0) {
        ownershipWeight = HeapTraverseNode.RefWeight.NON_COMPONENT;
      }

      HeapTraverseNode currentNode = objectIdToTraverseNode.get(objectId);
      if (currentNode == null) {
        currentNode = new HeapTraverseNode(value);

        currentNode.myOwnershipWeight = ownershipWeight;
        currentNode.myOwnedByComponentMask = parentNode.myOwnedByComponentMask;

        currentNode.myRetainedMask = parentNode.myRetainedMask;
        currentNode.myRetainedMaskForCategories = parentNode.myRetainedMaskForCategories;

        objectIdToTraverseNode.put(objectId, currentNode);
      }

      currentNode.myRetainedMask &= parentNode.myRetainedMask;
      currentNode.myRetainedMaskForCategories &= parentNode.myRetainedMaskForCategories;

      if (ownershipWeight.compareTo(currentNode.myOwnershipWeight) > 0) {
        currentNode.myOwnershipWeight = ownershipWeight;
        currentNode.myOwnedByComponentMask = parentNode.myOwnedByComponentMask;
      }
      else if (ownershipWeight.compareTo(currentNode.myOwnershipWeight) == 0) {
        currentNode.myOwnedByComponentMask |= parentNode.myOwnedByComponentMask;
      }
    }, fieldCache);
  }

  public static void collectAndPrintMemoryReport() {
    HeapSnapshotStatistics stats = new HeapSnapshotStatistics(ComponentsSet.getComponentSet());
    new HeapSnapshotTraverse(stats).walkObjects(MAX_DEPTH, getRoots());
    stats.print(new PrintWriter(System.out, true, StandardCharsets.UTF_8));
  }

  public static StatusCode collectMemoryReport() {
    HeapSnapshotStatistics stats = new HeapSnapshotStatistics(ComponentsSet.getComponentSet());
    long startTime = System.nanoTime();
    StatusCode statusCode =
      new HeapSnapshotTraverse(stats).walkObjects(MAX_DEPTH, getRoots());
    UsageTracker.log(AndroidStudioEvent.newBuilder()
                       .setKind(AndroidStudioEvent.EventKind.MEMORY_USAGE_REPORT_EVENT)
                       .setMemoryUsageReportEvent(
                         stats.buildMemoryUsageReportEvent(statusCode, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime),
                                                           TimeUnit.NANOSECONDS.toMillis(startTime))));
    return statusCode;
  }

  @NotNull
  private static WeakList<?> getRoots() {
    ClassLoader classLoader = HeapSnapshotTraverse.class.getClassLoader();
    // inspect static fields of all loaded classes
    @SuppressWarnings("UseOfObsoleteCollectionType")
    Vector<?> allLoadedClasses = ReflectionUtil.getField(classLoader.getClass(), classLoader, Vector.class, "classes");

    WeakList<Object> result = new WeakList<>();
    Application application = ApplicationManager.getApplication();
    if (application != null) {
      result.add(application);
    }
    result.add(Disposer.getTree());
    result.add(IdeEventQueue.getInstance());
    result.add(LaterInvocator.getLaterInvocatorEdtQueue());
    if (allLoadedClasses != null) {
      result.add(allLoadedClasses);
    }
    return result;
  }

  private static @NotNull String getLibName() {
    return System.mapLibraryName(JNI_OBJECT_TAGGER_LIB_NAME);
  }

  private static @NotNull String getPlatformName() {
    if (SystemInfo.isWindows) {
      return "win";
    }
    if (SystemInfo.isMac) {
      return CpuArch.isArm64() ? "mac_arm" : "mac";
    }
    if (SystemInfo.isLinux) {
      return "linux";
    }
    return "";
  }

  private static @NotNull Path getLibLocation() throws HeapSnapshotTraverseException {
    String libName = getLibName();
    Path homePath = Paths.get(PathManager.getHomePath());
    // Installed Studio.
    Path libFile = homePath.resolve(RESOURCES_NATIVE_PATH).resolve(libName);
    if (Files.exists(libFile)) {
      return libFile;
    }

    if (isRunningFromSources()) {
      // Dev environment.
      libFile = StudioPathManager.resolvePathFromSourcesRoot(DIAGNOSTICS_HEAP_NATIVE_PATH).resolve(getPlatformName()).resolve(libName);
      if (Files.exists(libFile)) {
        return libFile;
      }
    }
    throw new HeapSnapshotTraverseException(StatusCode.AGENT_LOAD_FAILED);
  }

  private static short getNextIterationId() {
    return ++ourIterationId;
  }

  private static void loadObjectTaggingAgent() throws HeapSnapshotTraverseException, IOException {
    String vmName = ManagementFactory.getRuntimeMXBean().getName();
    String pid = vmName.substring(0, vmName.indexOf('@'));
    VirtualMachine vm = null;
    try {
      vm = VirtualMachine.attach(pid);
      vm.loadAgentPath(getLibLocation().toString());
    }
    catch (AttachNotSupportedException | AgentInitializationException | AgentLoadException e) {
      throw new HeapSnapshotTraverseException(StatusCode.AGENT_LOAD_FAILED);
    }
    finally {
      if (vm != null) {
        vm.detach();
      }
    }
  }

  private static native long getObjectTag(@NotNull final Object obj);

  private static native void setObjectTag(@NotNull final Object obj, long newTag);

  private static native boolean canTagObjects();

  private static native long getObjectSize(@NotNull final Object obj);

  private static final class Node {
    private final int myDepth;
    @NotNull private final WeakReference<Object> myObjReference;
    private boolean myReferencesProcessed = false;

    private Node(@NotNull final Object obj, int depth) {
      myObjReference = new WeakReference<>(obj);
      myDepth = depth;
    }

    @Nullable
    private Object getObject() {
      return myObjReference.get();
    }

    private int getDepth() {
      return myDepth;
    }
  }
}
