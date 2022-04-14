/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.profilers.cpu.capturedetails

import com.android.tools.adtui.model.Range
import com.android.tools.perflib.vmtrace.ClockType
import com.android.tools.profilers.cpu.CaptureNode
import com.android.tools.profilers.cpu.nodemodel.CaptureNodeModel

abstract class CpuTreeNode<T : CpuTreeNode<T>?>(val id: String) {
  /**
   * References to [CaptureNode] that are used to extract information from to represent this CpuTreeNode,
   * such as [.getGlobalTotal], [.getGlobalChildrenTotal], etc...
   */
  private val nodeList = mutableListOf<CaptureNode>()
  private val childrenList = mutableListOf<T>()
  var globalTotal = 0.0
    protected set
  var globalChildrenTotal = 0.0
    protected set
  var threadTotal = 0.0
    protected set
  var threadChildrenTotal = 0.0
    protected set

  val nodes: List<CaptureNode> get() = nodeList
  val children: List<T> get() = childrenList

  abstract val methodModel: CaptureNodeModel
  abstract val filterType: CaptureNode.FilterType
  val isUnmatched: Boolean get() = filterType === CaptureNode.FilterType.UNMATCH

  protected fun addNode(node: CaptureNode) = nodeList.add(node)
  protected fun addNodes(nodes: List<CaptureNode>) = nodes.forEach(::addNode)
  protected fun addChild(child: T) = childrenList.add(child)

  fun getTotal(clockType: ClockType): Double = when (clockType) {
    ClockType.GLOBAL -> globalTotal
    ClockType.THREAD -> threadTotal
  }

  fun getChildrenTotal(clockType: ClockType): Double = when (clockType) {
    ClockType.GLOBAL -> globalChildrenTotal
    ClockType.THREAD -> threadChildrenTotal
  }

  fun getSelf(clockType: ClockType): Double = getTotal(clockType) - getChildrenTotal(clockType)

  open fun update(clockType: ClockType, range: Range) {
    reset()
    for (node in nodeList) {
      globalTotal += getIntersection(range, node, ClockType.GLOBAL)
      threadTotal += getIntersection(range, node, ClockType.THREAD)
      for (child in node.children) {
        globalChildrenTotal += getIntersection(range, child, ClockType.GLOBAL)
        threadChildrenTotal += getIntersection(range, child, ClockType.THREAD)
      }
    }
  }

  fun inRange(range: Range): Boolean = nodeList.any { it.start < range.max && range.min < it.end }

  fun reset() {
    globalTotal = 0.0
    globalChildrenTotal = 0.0
    threadTotal = 0.0
    threadChildrenTotal = 0.0
  }

  companion object {
    @JvmStatic
    protected fun getIntersection(range: Range, node: CaptureNode, type: ClockType): Double = when (type) {
      ClockType.GLOBAL -> range.getIntersectionLength(node.startGlobal.toDouble(), node.endGlobal.toDouble())
      ClockType.THREAD -> range.getIntersectionLength(node.startThread.toDouble(), node.endThread.toDouble())
    }

    /**
     * Return a pair of fresh mutable maps indexed by booleans
     */
    internal fun<K, V> mapPair(): (Boolean) -> MutableMap<K, V> {
      val onT = hashMapOf<K, V>()
      val onF = hashMapOf<K, V>()
      return { if (it) onT else onF }
    }
  }
}