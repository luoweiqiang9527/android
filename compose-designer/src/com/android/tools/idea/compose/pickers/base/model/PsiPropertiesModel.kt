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
package com.android.tools.idea.compose.pickers.base.model

import com.android.tools.idea.compose.pickers.base.inspector.PsiPropertiesInspectorBuilder
import com.android.tools.idea.compose.pickers.base.property.PsiPropertyItem
import com.android.tools.idea.compose.pickers.base.tracking.ComposePickerTracker
import com.android.tools.idea.util.ListenerCollection
import com.android.tools.property.panel.api.PropertiesModel
import com.android.tools.property.panel.api.PropertiesModelListener
import com.android.tools.property.panel.api.PropertiesView

private const val PSI_PROPERTIES_VIEW_NAME = "PsiProperties"

/** Base [PropertiesModel] for pickers interacting with PSI elements. */
internal abstract class PsiPropertiesModel : PropertiesModel<PsiPropertyItem> {
  private val listeners =
    ListenerCollection.createWithDirectExecutor<PropertiesModelListener<PsiPropertyItem>>()

  /**
   * Builder to generate the properties Table UI.
   *
   * May define how the properties are organized, add headers or sections; and set custom editors.
   */
  abstract val inspectorBuilder: PsiPropertiesInspectorBuilder

  /** Usage tracker, called on every [PsiPropertyItem] modification. */
  abstract val tracker: ComposePickerTracker

  override fun addListener(listener: PropertiesModelListener<PsiPropertyItem>) {
    // For now, the properties are always generated at load time, so we can always make this call
    // when the listener is added.
    listener.propertiesGenerated(this)
    listeners.add(listener)
  }

  override fun removeListener(listener: PropertiesModelListener<PsiPropertyItem>) {
    listeners.remove(listener)
  }

  internal fun firePropertyValuesChanged() {
    listeners.forEach { it.propertyValuesChanged(this) }
  }

  override fun deactivate() {}
}

/** A [PropertiesView] for editing [PsiPropertiesModel]s. */
internal class PsiPropertyView(model: PsiPropertiesModel) :
  PropertiesView<PsiPropertyItem>(PSI_PROPERTIES_VIEW_NAME, model) {

  init {
    addTab("").apply { builders.add(model.inspectorBuilder) }
  }
}
