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
package com.android.tools.idea.layoutinspector.legacydevice

import com.android.ddmlib.ChunkHandler
import com.android.ddmlib.Client
import com.android.ddmlib.HandleViewDebug
import com.android.tools.idea.layoutinspector.model.TreeLoader
import com.android.tools.idea.layoutinspector.model.ViewNode
import com.android.tools.idea.layoutinspector.resource.ResourceLookup
import com.android.tools.idea.layoutinspector.transport.InspectorClient
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Charsets
import com.google.common.collect.Lists
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.util.Stack
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector
import javax.imageio.ImageIO

/**
 * A [TreeLoader] that can handle pre-api 29 devices. Loads the view hierarchy and screenshot using DDM, and parses it into [ViewNode]s
 */
object LegacyTreeLoader : TreeLoader {

  // Reverse mapping of hashCode
  private val latestWindowIds = mutableMapOf<Long, String>()

  override fun loadComponentTree(data: Any?, resourceLookup: ResourceLookup, client: InspectorClient): ViewNode? {
    val legacyClient = client as? LegacyClient ?: return null
    val ddmClient = legacyClient.selectedClient ?: return null
    val (maybeWindow, maybeUpdater) = data as? Pair<*, *> ?: return null
    val window = maybeWindow as? Long ?: return null
    val updater = maybeUpdater as? LegacyPropertiesProvider.Updater ?: return null
    val windowName = latestWindowIds[window] ?: return null
    return capture(ddmClient, windowName, updater)
  }

  override fun getAllWindowIds(data: Any?, client: InspectorClient): List<Long>? {
    val legacyClient = client as? LegacyClient ?: return null
    val ddmClient = legacyClient.selectedClient ?: return null
    val windows = ListViewRootsHandler().getWindows(ddmClient, 5, TimeUnit.SECONDS)
    latestWindowIds.clear()
    windows.associateByTo(latestWindowIds) { it.hashCode().toLong() }
    return windows.map { it.hashCode().toLong() }
  }

  private fun capture(client: Client, window: String, propertiesUpdater: LegacyPropertiesProvider.Updater): ViewNode? {
    val hierarchyHandler = CaptureByteArrayHandler(HandleViewDebug.CHUNK_VURT)
    HandleViewDebug.dumpViewHierarchy(client, window, false, true, false, hierarchyHandler)
    val hierarchyData = hierarchyHandler.getData() ?: return null
    val (rootNode, hash) = parseLiveViewNode(hierarchyData, propertiesUpdater) ?: return null
    rootNode.drawId = window.hashCode().toLong()
    val imageHandler = CaptureByteArrayHandler(HandleViewDebug.CHUNK_VUOP)
    HandleViewDebug.captureView(client, window, hash, imageHandler)
    try {
      val imageData = imageHandler.getData()
      rootNode.imageBottom = ImageIO.read(ByteArrayInputStream(imageData))
    }
    catch (e: IOException) {
      return null
    }
    return rootNode
  }

  private class CaptureByteArrayHandler(type: Int) : HandleViewDebug.ViewDumpHandler(type) {

    private val mData = AtomicReference<ByteArray>()

    override fun handleViewDebugResult(data: ByteBuffer) {
      val b = ByteArray(data.remaining())
      data.get(b)
      mData.set(b)
    }

    fun getData(): ByteArray? {
      waitForResult(15, TimeUnit.SECONDS)
      return mData.get()
    }
  }

  /** Parses the flat string representation of a view node and returns the root node.  */
  @VisibleForTesting
  fun parseLiveViewNode(bytes: ByteArray, propertyUpdater: LegacyPropertiesProvider.Updater): Pair<ViewNode, String>?  {
    var rootNodeAndHash: Pair<ViewNode, String>? = null
    var lastNodeAndHash: Pair<ViewNode, String>? = null
    var lastWhitespaceCount = Integer.MIN_VALUE
    val stack = Stack<ViewNode>()

    val input = BufferedReader(
      InputStreamReader(ByteArrayInputStream(bytes), Charsets.UTF_8)
    )

    for (line in input.lines().collect(MergeNewLineCollector)) {
      if ("DONE.".equals(line, ignoreCase = true)) {
        break
      }
      // determine parent through the level of nesting by counting whitespaces
      var whitespaceCount = 0
      while (line[whitespaceCount] == ' ') {
        whitespaceCount++
      }

      if (lastWhitespaceCount < whitespaceCount) {
        stack.push(lastNodeAndHash?.first)
      }
      else if (!stack.isEmpty()) {
        val count = lastWhitespaceCount - whitespaceCount
        for (i in 0 until count) {
          stack.pop()
        }
      }

      lastWhitespaceCount = whitespaceCount
      var parent: ViewNode? = null
      if (!stack.isEmpty()) {
        parent = stack.peek()
      }
      lastNodeAndHash = createViewNode(parent, line.trim(), propertyUpdater)
      if (rootNodeAndHash == null) {
        rootNodeAndHash = lastNodeAndHash
      }
    }

    return rootNodeAndHash
  }

  private fun createViewNode(parent: ViewNode?, data: String, propertyLoader: LegacyPropertiesProvider.Updater): Pair<ViewNode, String> {
    val (name, dataWithoutName) = data.split('@', limit = 2)
    val (hash, properties) = dataWithoutName.split(' ', limit = 2)
    val hashId = hash.toLongOrNull(16) ?: 0
    val view = ViewNode(hashId, name, null, 0, 0, 0, 0, 0, 0, null, "", 0)
    view.parent = parent
    parent?.children?.add(view)
    propertyLoader.parseProperties(view, properties)
    return Pair(view, "$name@$hash")
  }

  /**
   * A custom collector that handles a special case see b/79183623
   * If a text field has text containing a new line it'll cause the view node output to be split
   * across multiple lines so the collector processes the file output and merges those back into a
   * single line so we can correctly create view nodes.
   */
  private object MergeNewLineCollector : Collector<String, MutableList<String>, List<String>> {
    override fun characteristics(): Set<Collector.Characteristics> {
      return setOf(Collector.Characteristics.CONCURRENT)
    }

    override fun supplier() = Supplier<MutableList<String>> { mutableListOf() }
    override fun finisher() = Function<MutableList<String>, List<String>> { it.toList() }
    override fun combiner() =
      BinaryOperator<MutableList<String>> { t, u -> t.apply { addAll(u) } }

    override fun accumulator() = BiConsumer<MutableList<String>, String> { stringGroup, line ->
      val newLine = line.trim()
      // add the original line because we need to keep the spacing to determine hierarchy
      if (newLine.startsWith("\\n")) {
        stringGroup[stringGroup.lastIndex] = stringGroup.last() + line
      }
      else {
        stringGroup.add(line)
      }
    }
  }

  private class ListViewRootsHandler :
    HandleViewDebug.ViewDumpHandler(HandleViewDebug.CHUNK_VULW) {

    private val viewRoots = Lists.newCopyOnWriteArrayList<String>()

    override fun handleViewDebugResult(data: ByteBuffer) {
      val nWindows = data.int

      for (i in 0 until nWindows) {
        val len = data.int
        viewRoots.add(ChunkHandler.getString(data, len))
      }
    }

    @Throws(IOException::class)
    fun getWindows(c: Client, timeout: Long, unit: TimeUnit): List<String> {
      HandleViewDebug.listViewRoots(c, this)
      waitForResult(timeout, unit)
      return viewRoots
    }
  }
}
