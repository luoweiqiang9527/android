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
package com.android.tools.idea.npw.module.recipes.automotiveModule

import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor
import com.android.tools.idea.npw.module.recipes.generateCommonModule
import com.android.tools.idea.npw.module.recipes.generateManifest

fun RecipeExecutor.generateAutomotiveModule(
  data: ModuleTemplateData,
  appTitle: String
) {
  val usesFeatureBlock = """
<uses-feature
    android:name="android.hardware.type.automotive"
    android:required="true"/>
"""
  generateCommonModule(
    data, appTitle,
    generateManifest(data.packageName, !data.isLibrary, usesFeatureBlock = usesFeatureBlock),
    true
  )
  addDependency("com.android.support:appcompat-v7:${data.apis.buildApi}.+")
}
