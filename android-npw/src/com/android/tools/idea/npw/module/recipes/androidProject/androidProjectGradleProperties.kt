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
package com.android.tools.idea.npw.module.recipes.androidProject

import com.android.tools.idea.wizard.template.renderIf

fun androidProjectGradleProperties(
  addAndroidXSupport: Boolean,
  generateKotlin: Boolean,
  overridePathCheck: Boolean?,
): String {
  val androidXBlock = renderIf(addAndroidXSupport) { """
# AndroidX package structure to make it clearer which packages are bundled with the
# Android operating system, and which are packaged with your app's APK
# https://developer.android.com/topic/libraries/support-library/androidx-rn
android.useAndroidX=true
"""
  }

  val kotlinStyleBlock = renderIf(generateKotlin) { """
# Kotlin code style for this project: "official" or "obsolete":
kotlin.code.style=official
"""
  }

  val overridePathCheckBlock = renderIf(overridePathCheck != null) {
    """
# Allow non-ASCII characters in project path on Windows
android.overridePathCheck=$overridePathCheck
"""
  }

  val nonTransitiveRClass =  """
# Enables namespacing of each library's R class so that its R class includes only the
# resources declared in the library itself and none from the library's dependencies,
# thereby reducing the size of the R class for that library
android.nonTransitiveRClass=true
"""

  return  """
# Project-wide Gradle settings.

# IDE (e.g. Android Studio) users:
# Gradle settings configured through the IDE *will override*
# any settings specified in this file.

# For more details on how to configure your build environment visit
# http://www.gradle.org/docs/current/userguide/build_environment.html

# Specifies the JVM arguments used for the daemon process.
# The setting is particularly useful for tweaking memory settings.
org.gradle.jvmargs=-Xmx${maxHeapSize}m -Dfile.encoding=UTF-8

# When configured, Gradle will run in incubating parallel mode.
# This option should only be used with decoupled projects. For more details, visit
# https://developer.android.com/r/tools/gradle-multi-project-decoupled-projects
# org.gradle.parallel=true

$androidXBlock
$kotlinStyleBlock
$overridePathCheckBlock
$nonTransitiveRClass
"""
}

private val maxHeapSize = if (System.getProperty("sun.arch.data.model") == "32") 1024 else 2048
