/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.testing

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.mockito.MockedStatic
import org.mockito.Mockito

/**
 * Mocks static method invocations within the current thread. The lifetime of the mock is controlled by the given [disposable].
 *
 * See also: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html#48
 */
inline fun <reified T> mockStatic(disposable: Disposable): MockedStatic<T> {
  return Mockito.mockStatic(T::class.java).also { mock -> Disposer.register(disposable) { mock.close() } }
}