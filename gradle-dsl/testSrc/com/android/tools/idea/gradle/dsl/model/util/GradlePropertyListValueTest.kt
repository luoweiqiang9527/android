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
package com.android.tools.idea.gradle.dsl.model.util

import com.android.tools.idea.gradle.dsl.TestFileName
import com.android.tools.idea.gradle.dsl.api.ext.GradlePropertyModel.LIST_TYPE
import com.android.tools.idea.gradle.dsl.api.ext.PropertyType.REGULAR
import com.android.tools.idea.gradle.dsl.model.GradleFileModelTestCase
import org.jetbrains.annotations.SystemDependent
import org.junit.Test
import java.io.File

class GradlePropertyListValueTest : GradleFileModelTestCase() {
  @Test
  fun testReplaceListValue() {
    writeToBuildFile(TestFile.REPLACE_LIST_VALUE)

    val buildModel = gradleBuildModel

    run {
      val firstModel = buildModel.ext().findProperty("prop1")
      verifyListProperty(firstModel, listOf(1, 2, 2, 4), REGULAR, 0)

      replaceListValue(firstModel, 2, 3)
      replaceListValue(firstModel, 2, 3)
      replaceListValue(firstModel, 4, 7)

      verifyListProperty(firstModel, listOf(1, 3, 3, 7), REGULAR, 0)

      val secondModel = buildModel.ext().findProperty("prop2")
      verifyListProperty(secondModel, listOf("a", "b", "b", "d"), REGULAR, 0)

      replaceListValue(secondModel, "d", "a")

      verifyListProperty(secondModel, listOf("a", "b", "b", "a"), REGULAR, 0)

      val thirdModel = buildModel.ext().findProperty("prop3")
      verifyListProperty(thirdModel, listOf(true, true, false, true), REGULAR, 0)

      replaceListValue(thirdModel, false, true)

      verifyListProperty(thirdModel, listOf(true, true, true, true), REGULAR, 0)
    }

    applyChangesAndReparse(buildModel)
    verifyFileContents(myBuildFile, TestFile.REPLACE_LIST_VALUE_EXPECTED)

    run {
      val firstModel = buildModel.ext().findProperty("prop1")
      verifyListProperty(firstModel, listOf(1, 3, 3, 7), REGULAR, 0)
      val secondModel = buildModel.ext().findProperty("prop2")
      verifyListProperty(secondModel, listOf("a", "b", "b", "a"), REGULAR, 0)
      val thirdModel = buildModel.ext().findProperty("prop3")
      verifyListProperty(thirdModel, listOf(true, true, true, true), REGULAR, 0)
    }
  }

  @Test
  fun testReplaceListValueOnNoneList() {
    writeToBuildFile(TestFile.REPLACE_LIST_VALUE_ON_NONE_LIST)

    val buildModel = gradleBuildModel

    run {
      val firstModel = buildModel.ext().findProperty("prop1")
      replaceListValue(firstModel, "value1", "newValue")

      val secondModel = buildModel.ext().findProperty("prop2")
      replaceListValue(secondModel, "hello", "goodbye")

      val thirdModel = buildModel.ext().findProperty("prop3")
      replaceListValue(thirdModel, 1, 0)
    }

    assertFalse(buildModel.isModified)
  }

  @Test
  fun testRemoveListValues() {
    writeToBuildFile(TestFile.REMOVE_LIST_VALUES)

    val buildModel = gradleBuildModel

    run {
      val firstModel = buildModel.ext().findProperty("prop1")
      removeListValue(firstModel, 1)
      removeListValue(firstModel, 2)

      val secondModel = buildModel.ext().findProperty("prop2")
      removeListValue(secondModel, "b")

      val thirdModel = buildModel.ext().findProperty("prop3")
      removeListValue(thirdModel, true)
      removeListValue(thirdModel, true)

      verifyListProperty(firstModel, listOf(), REGULAR, 0)
      verifyListProperty(secondModel, listOf("a", "b", "d"), REGULAR, 0)
      verifyListProperty(thirdModel, listOf(false, true), REGULAR, 0)
    }

    applyChangesAndReparse(buildModel)
    // TODO(b/148198247): the empty list needs a type decorator.  But what?  It depends on how the variable is used in the rest of
    //  the build configuration.
    verifyFileContents(myBuildFile, TestFile.REMOVE_LIST_VALUES_EXPECTED)

    run {
      val firstModel = buildModel.ext().findProperty("prop1")
      val secondModel = buildModel.ext().findProperty("prop2")
      val thirdModel = buildModel.ext().findProperty("prop3")
      verifyListProperty(firstModel, listOf(), REGULAR, 0)
      verifyListProperty(secondModel, listOf("a", "b", "d"), REGULAR, 0)
      verifyListProperty(thirdModel, listOf(false, true), REGULAR, 0)
    }
  }

  @Test
  fun testSetListValue() {
    writeToBuildFile(TestFile.SET_LIST_VALUE)
    val buildModel = gradleBuildModel
    val prop1 = buildModel.ext().findProperty("prop1")
    val prop2 = buildModel.ext().findProperty("prop2")
    val prop1Value = prop1.getValue(LIST_TYPE)
    assertNotNull(prop1Value)
    prop2.setValue(prop1Value!!)

    applyChangesAndReparse(buildModel)
    verifyFileContents(myBuildFile, TestFile.SET_LIST_VALUE_EXPECTED)
  }

  enum class TestFile(val path: @SystemDependent String): TestFileName {
    REPLACE_LIST_VALUE("replaceListValue"),
    REPLACE_LIST_VALUE_EXPECTED("replaceListValueExpected"),
    REPLACE_LIST_VALUE_ON_NONE_LIST("replaceListValueOnNoneList"),
    REMOVE_LIST_VALUES("removeListValues"),
    REMOVE_LIST_VALUES_EXPECTED("removeListValuesExpected"),
    SET_LIST_VALUE("setListValue"),
    SET_LIST_VALUE_EXPECTED("setListValueExpected"),
    ;
    override fun toFile(basePath: @SystemDependent String, extension: String): File {
      return super.toFile("$basePath/gradlePropertyListValue/$path", extension)
    }
  }
}