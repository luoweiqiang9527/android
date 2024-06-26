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
package com.android.tools.idea.common.error

import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.testing.onEdt
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.assertInstanceOf
import com.intellij.util.concurrency.InvokerSupplier
import org.junit.Rule
import org.junit.Test

class DesignerCommonIssuePanelModelProviderTest {

  @Rule @JvmField val rule = AndroidProjectRule.inMemory().onEdt()

  @Test
  fun testAsyncDesignerCommonIssuePanelModelProvider() {
    val provider = AsyncDesignerCommonIssuePanelModelProvider()
    val model = provider.createModel()

    // The async provider must be a InvokerSupplier
    assertInstanceOf<InvokerSupplier>(model)

    // Invoker.toString() gets the description of invoker.
    // A background invoker should contain "Background.Thread" in its description
    (model as InvokerSupplier).invoker.toString().contains("Background.Thread")

    Disposer.dispose(model)
  }
}
