// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.android.synthetic.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

internal val Module.isAndroidModule
    get() = AndroidModuleInfoProvider.getInstance(this)?.isAndroidModule() ?: false

@Deprecated("This is used exclusively by the deprecated Kotlin synthetics code, and it will be deleted soon")
interface AndroidModuleInfoProvider {
    companion object {
        val EP_NAME = ExtensionPointName<AndroidModuleInfoProvider>("org.jetbrains.kotlin.android.model.androidModuleInfoProvider")

        fun getInstance(module: Module): AndroidModuleInfoProvider? {
            val extensionArea = module.extensionArea
            if (!extensionArea.hasExtensionPoint(EP_NAME.name)) {
                return null
            }
            return extensionArea.getExtensionPoint(EP_NAME).extensionList.firstOrNull()
        }

        fun getInstance(element: PsiElement): AndroidModuleInfoProvider? {
            val module = ApplicationManager.getApplication().runReadAction<Module> {
                ModuleUtilCore.findModuleForPsiElement(element)
            }
            return getInstance(module)
        }
    }

    val module: Module

    fun isAndroidModule(): Boolean
    fun isGradleModule(): Boolean

    fun getApplicationPackage(): String?

    @Deprecated("Do not use. IDEAndroidLayoutXmlFileManager requires this method for compatibility reasons")
    fun getMainAndFlavorSourceProviders(): List<SourceProviderMirror>
    fun getAllResourceDirectories(): List<VirtualFile>

    // For experimental Android Extensions
    fun getActiveSourceProviders(): List<SourceProviderMirror>

    interface SourceProviderMirror {
        val name: String
        val resDirectories: Collection<VirtualFile>
    }
}

