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
package com.android.tools.idea.layoutinspector

import com.android.SdkConstants.CLASS_VIEW
import com.android.ide.common.rendering.api.ResourceReference
import com.android.testutils.MockitoKt.mock
import com.android.tools.idea.layoutinspector.model.ComposeViewNode
import com.android.tools.idea.layoutinspector.model.DrawViewChild
import com.android.tools.idea.layoutinspector.model.DrawViewImage
import com.android.tools.idea.layoutinspector.model.InspectorModel
import com.android.tools.idea.layoutinspector.model.ViewNode
import com.android.tools.idea.layoutinspector.util.ConfigurationBuilder
import com.android.tools.idea.layoutinspector.util.TestStringTable
import com.android.tools.layoutinspector.proto.LayoutInspectorProto.ComponentTreeEvent.PayloadType
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import java.awt.Image
import java.awt.Rectangle
import java.awt.image.BufferedImage

// TODO: find a way to indicate that this is a api 29+ model without having to specify an image on a subnode
fun model(project: Project = mock(), body: InspectorModelDescriptor.() -> Unit) =
  InspectorModelDescriptor(project).also(body).build()

fun view(drawId: Long,
         x: Int = 0,
         y: Int = 0,
         width: Int = 0,
         height: Int = 0,
         qualifiedName: String = CLASS_VIEW,
         viewId: ResourceReference? = null,
         textValue: String = "",
         layoutFlags: Int = 0,
         layout: ResourceReference? = null,
         imageType: PayloadType = PayloadType.SKP,
         body: InspectorViewDescriptor.() -> Unit = {}) =
  InspectorViewDescriptor(drawId, qualifiedName, x, y, width, height, viewId, textValue, layoutFlags, layout, imageType)
    .also(body).build()

fun compose(drawId: Long,
            name: String,
            composeFilename: String = "",
            composePackageHash: Int = -1,
            composeOffset: Int = 0,
            composeLineNumber: Int = 0,
            x: Int = 0,
            y: Int = 0,
            width: Int = 0,
            height: Int = 0,
            imageType: PayloadType = PayloadType.SKP,
            body: InspectorViewDescriptor.() -> Unit = {}) =
  InspectorViewDescriptor(drawId, name, x, y, width, height, null, "", 0, null, imageType,
                          composeFilename, composePackageHash, composeOffset, composeLineNumber).also(body)

interface InspectorNodeDescriptor
class InspectorImageDescriptor(
  internal val image: BufferedImage, internal val x: Int? = null, internal val y: Int? = null
): InspectorNodeDescriptor

class InspectorViewDescriptor(private val drawId: Long,
                              private val qualifiedName: String,
                              private val x: Int,
                              private val y: Int,
                              private val width: Int,
                              private val height: Int,
                              private val viewId: ResourceReference?,
                              private val textValue: String,
                              private val layoutFlags: Int,
                              private val layout: ResourceReference?,
                              private val imageType: PayloadType,
                              private val composeFilename: String = "",
                              private val composePackageHash: Int = 0,
                              private val composeOffset: Int = 0,
                              private val composeLineNumber: Int = 0): InspectorNodeDescriptor {
  private val children = mutableListOf<InspectorNodeDescriptor>()

  fun image(image: BufferedImage = mock()) {
    children.add(InspectorImageDescriptor(image, x, y))
  }

  fun view(drawId: Long,
           x: Int = 0,
           y: Int = 0,
           width: Int = 0,
           height: Int = 0,
           qualifiedName: String = CLASS_VIEW,
           viewId: ResourceReference? = null,
           textValue: String = "",
           layoutFlags: Int = 0,
           layout: ResourceReference? = null,
           imageType: PayloadType = PayloadType.SKP,
           body: InspectorViewDescriptor.() -> Unit = {}) =
    children.add(InspectorViewDescriptor(
      drawId, qualifiedName, x, y, width, height, viewId, textValue, layoutFlags, layout, imageType).apply(body))

  fun view(drawId: Long,
           rect: Rectangle,
           qualifiedName: String = CLASS_VIEW,
           viewId: ResourceReference? = null,
           textValue: String = "",
           layout: ResourceReference? = null,
           imageType: PayloadType = PayloadType.SKP,
           body: InspectorViewDescriptor.() -> Unit = {}) =
    view(drawId, rect.x, rect.y, rect.width, rect.height, qualifiedName, viewId, textValue, 0, layout, imageType, body)

  fun compose(drawId: Long,
              name: String,
              composeFilename: String = "",
              composePackageHash: Int = -1,
              composeOffset: Int = 0,
              composeLineNumber: Int = 0,
              x: Int = 0,
              y: Int = 0,
              width: Int = 0,
              height: Int = 0,
              imageType: PayloadType = PayloadType.SKP,
              body: InspectorViewDescriptor.() -> Unit = {}) =
    children.add(InspectorViewDescriptor(drawId, name, x, y, width, height, null, "", 0, null, imageType,
                                         composeFilename, composePackageHash, composeOffset, composeLineNumber).apply(body))

  fun build(): ViewNode {
    val result =
      if (composePackageHash == 0) ViewNode(drawId, qualifiedName, layout, x, y, width, height, null, viewId, textValue, layoutFlags)
      else ComposeViewNode(drawId, qualifiedName, null, x, y, width, height, null, textValue, 0,
                           composeFilename, composePackageHash, composeOffset, composeLineNumber)
    result.imageType = imageType
    children.forEach {
      when (it) {
        is InspectorViewDescriptor -> {
          val viewNode = it.build()
          result.children.add(viewNode)
          result.drawChildren.add(DrawViewChild(viewNode))
        }
        is InspectorImageDescriptor -> {
          val image = it.image.getSubimage(0, 0, result.width, result.height) ?: it.image
          result.drawChildren.add(DrawViewImage(image, it.x ?: result.x, it.y ?: result.y, result))
        }
      }
    }
    result.children.forEach { it.parent = result }
    return result
  }
}

class InspectorModelDescriptor(val project: Project) {
  private var root: InspectorViewDescriptor? = null

  fun view(drawId: Long,
           x: Int = 0,
           y: Int = 0,
           width: Int = 0,
           height: Int = 0,
           qualifiedName: String = CLASS_VIEW,
           viewId: ResourceReference? = null,
           textValue: String = "",
           layoutFlags: Int = 0,
           layout: ResourceReference? = null,
           imageType: PayloadType = PayloadType.SKP,
           body: InspectorViewDescriptor.() -> Unit = {}) {
    root = InspectorViewDescriptor(
      drawId, qualifiedName, x, y, width, height, viewId, textValue, layoutFlags, layout, imageType).apply(body)
  }

  fun view(drawId: Long,
           rect: Rectangle,
           qualifiedName: String = CLASS_VIEW,
           viewId: ResourceReference? = null,
           textValue: String = "",
           imageType: PayloadType = PayloadType.SKP,
           body: InspectorViewDescriptor.() -> Unit = {}) =
    view(drawId, rect.x, rect.y, rect.width, rect.height, qualifiedName, viewId, textValue, 0, null, imageType, body)

  fun build(): InspectorModel {
    val model = InspectorModel(project)
    val windowRoot = root?.build() ?: return model
    model.update(windowRoot, windowRoot.drawId, listOf(windowRoot.drawId))
    if (ModuleManager.getInstance(project) != null) {
      val strings = TestStringTable()
      val config = ConfigurationBuilder(strings)
      model.resourceLookup.updateConfiguration(config.makeSampleConfiguration(project), strings)
    }
    return model
  }
}