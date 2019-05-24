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
package com.android.tools.idea.navigator

import com.android.tools.idea.Projects
import com.android.tools.idea.testing.AndroidGradleTestCase
import com.android.tools.idea.testing.SnapshotComparisonTest
import com.android.tools.idea.testing.TestProjectPaths
import com.android.tools.idea.testing.assertIsEqualToSnapshot
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.impl.GroupByTypeComparator
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.DeferredIcon
import com.intellij.ui.LayeredIcon
import com.intellij.ui.RowIcon
import java.io.File
import javax.swing.Icon

class AndroidGradleProjectViewSnapshotComparisonTest : AndroidGradleTestCase(), SnapshotComparisonTest {
  override val snapshotDirectoryName = "projectViewSnapshots"

  fun testSimpleApplication() {
    val text = importSyncAndDumpProject(TestProjectPaths.SIMPLE_APPLICATION)
    assertIsEqualToSnapshot(text)
  }

  fun testNestedProjects() {
    val text = importSyncAndDumpProject(TestProjectPaths.PSD_SAMPLE)
    assertIsEqualToSnapshot(text)
  }

  private fun importSyncAndDumpProject(projectDir: String, patch: ((projectRootPath: File) -> Unit)? = null): String {
    val projectRootPath = prepareProjectForImport(projectDir)
    patch?.invoke(projectRootPath)
    val project = this.project!!
    importProject(project.name, Projects.getBaseDirPath(project))

    return project.dumpAndroidProjectView()
  }

  private fun Project.dumpAndroidProjectView(): String {
    val viewPane = AndroidProjectViewPane(this)
    // We need to create a component to initialize the view pane.
    viewPane.createComponent()
    val treeStructure: AbstractTreeStructure? = viewPane.treeStructure
    val rootElement = treeStructure?.rootElement ?: return ""
    // In production sorting happens when the tree builder asynchronously populates the UI. It uses the following comparator, by default,
    // which, unfortunately, is not accessible via a public API.
    val comparator = GroupByTypeComparator(null, "android")

    return buildString {

      fun Icon.getIconText(): Icon? {
        var icon: Icon? = this
        do {
          val previous = icon
          icon = if (icon is DeferredIcon) icon.evaluate() else icon
          icon = if (icon is RowIcon && icon.allIcons.size == 1) icon.getIcon(0) else icon
          icon = if (icon is LayeredIcon && icon.allLayers.size == 1) icon.getIcon(0) else icon
        }
        while (previous != icon)
        return icon
      }

      fun PresentationData.toTestText(): String {
        val icon = getIcon(false)?.getIconText()
        val iconText = (icon as? IconLoader.CachedImageIcon)?.originalPath ?: icon?.let { "$it (${it.javaClass.simpleName})" }
        val nodeText =
          if (coloredText.isEmpty()) presentableText
          else coloredText.joinToString(separator = "") { it.text }

        return buildString {
          append(nodeText)
          if (iconText != null) append(" (icon: $iconText)")
        }
      }

      fun dump(element: AbstractTreeNode<*>, prefix: String = "") {
        appendln("$prefix${element.presentation.toTestText()}")
        treeStructure
          .getChildElements(element)
          .map { it as AbstractTreeNode<*> }
          .apply { forEach { it.update() } }
          .sortedWith(comparator)
          .forEach { dump(it, "    $prefix") }
      }

      dump(rootElement as AbstractTreeNode<*>)
    }
      // Trim the trailing line end since snapshots are loaded without it.
      .trimEnd()
  }
}