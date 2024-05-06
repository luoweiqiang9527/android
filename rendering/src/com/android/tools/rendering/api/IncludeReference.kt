/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.tools.rendering.api

import com.android.tools.rendering.parsers.RenderXmlFile
import com.intellij.openapi.project.Project
import java.io.File

/** A reference to a particular file in the project. */
interface IncludeReference {
  fun getFromXmlFile(project: Project): RenderXmlFile?

  val fromPath: File

  val fromResourceUrl: String

  companion object {
    @JvmField
    val NONE =
      object : IncludeReference {
        override fun getFromXmlFile(project: Project): RenderXmlFile? = null

        override val fromPath: File = File("")
        override val fromResourceUrl: String = ""
      }
  }
}
