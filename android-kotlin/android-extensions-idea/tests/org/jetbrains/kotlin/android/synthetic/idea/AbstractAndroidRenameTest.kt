/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.android.synthetic.idea

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.kotlin.android.KotlinAndroidTestCase

abstract class AbstractAndroidRenameTest : KotlinAndroidTestCase() {
    private val NEW_NAME = "NEWNAME"

    fun doTest(path: String) {
        copyResourceDirectoryForTest(path)
        val virtualFile = myFixture.copyFileToProject("$path${getTestName(true)}.kt", "src/${getTestName(true)}.kt")
        myFixture.configureFromExistingVirtualFile(virtualFile)
        myFixture.renameElement(myFixture.elementAtCaret, NEW_NAME)
        myFixture.checkResultByFile("$path/expected/${getTestName(true)}.kt")
        assertResourcesEqual("$testDataPath/$path/expected/res")
    }

    private fun assertResourcesEqual(expectedPath: String) {
        PlatformTestUtil.assertDirectoriesEqual(LocalFileSystem.getInstance().findFileByPath(expectedPath)!!, getResourceDirectory()!!)
    }

    private fun getResourceDirectory() = LocalFileSystem.getInstance().findFileByPath(myFixture.tempDirPath + "/res")
}
