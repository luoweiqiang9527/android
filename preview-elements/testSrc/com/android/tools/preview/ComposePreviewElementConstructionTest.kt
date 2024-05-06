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
package com.android.tools.preview

import com.android.tools.preview.config.PARAMETER_BACKGROUND_COLOR
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ComposePreviewElementConstructionTest {
  private class BackgroundColorProvider<B>(
    private val backgroundColor: B?
  ) : AnnotationAttributesProvider {
    override fun <T> getAttributeValue(attributeName: String): T? = null
    override fun getIntAttribute(attributeName: String): Int? = null
    override fun getStringAttribute(attributeName: String): String? = null
    override fun getFloatAttribute(attributeName: String): Float? = null
    override fun getBooleanAttribute(attributeName: String): Boolean? = null
    override fun <T> getDeclaredAttributeValue(attributeName: String): T? = when(attributeName) {
      PARAMETER_BACKGROUND_COLOR -> backgroundColor as? T
      else -> null
    }
    override fun findClassNameValue(name: String): String? = null
  }

  @Test
  fun testPreviewAnnotationToPreviewElement_backgroundColor() {
    val annotatedMethod = object : AnnotatedMethod {
      override val name = "Method"
      override val qualifiedName = "com.test.Method"
      override val psiPointer = null
      override val parameterAnnotations = emptyList<Pair<String, AnnotationAttributesProvider>>()
    }
    val encodedBackgroundColors = listOf<Any?>(123456, 123456L, "123456", 123456.0f, null)
    val expectedBackgroundColors = listOf("#1e240", "#1e240", "#1e240", null, null)

    encodedBackgroundColors.zip(expectedBackgroundColors).forEach { (encodedColor, expectedColor) ->
      val attributesProvider = BackgroundColorProvider(encodedColor)

      val previewElement = previewAnnotationToPreviewElement(
        attributesProvider,
        annotatedMethod,
        null,
        { instance, params -> ParametrizedComposePreviewElementTemplate(instance, params) { null } }
      )

      assertNotNull(previewElement)
      assertEquals(expectedColor, previewElement.displaySettings.backgroundColor)
    }
  }
}