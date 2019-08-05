/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.tools.idea.templates

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the Freemarker utility functions
 */
class FmUtilTest {
  @Test
  fun testStripSuffix() {
    // No-op test
    assertEquals("", "".stripSuffix("", false))
    assertEquals("", "".stripSuffix("foo", false))

    // Whole string test
    assertEquals("", "foo".stripSuffix( "foo", false))

    // Suffix test
    assertEquals("Foo", "FooBar".stripSuffix( "Bar", false))
    assertEquals("Foo", "FooBar".stripSuffix("Bar", false))

    // Double Suffix test
    assertEquals("Foo", "FooBarBar".stripSuffix("Bar", true))
    assertEquals("FooBar", "FooBarBar".stripSuffix( "Bar", false))
  }
}
