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
package com.android.tools.idea.lang.proguardR8

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Adds Proguard/R8 files to use scope for JVM classes, fields and methods.
 *
 * We need to extend the `useScope` for non-public [PsiMember]s (including [PsiClass]es and light fields and methods generated by Kotlin)
 * and [KtProperty] because they can be used in Proguard/R8 files.
 *
 * @see com.intellij.psi.search.PsiSearchHelper.getUseScope
 */
class ProguardR8UseScopeEnlarger : UseScopeEnlarger() {
  override fun getAdditionalUseScope(element: PsiElement): SearchScope? {
    if ((element is PsiMember || element is KtProperty && !element.isLocal) && element.containingFile != null) {
      val project = element.project

      if (ApplicationManager.getApplication().isDispatchThread) {
        with (DumbService.getInstance(project)) {
          // When indexing is happening and alternative resolve is enabled, FileTypeIndex will wait for indexing to be complete.
          // Return null here to avoid a deadlock if this is being called from the event thread.
          if (isDumb && isAlternativeResolveEnabled) return null
        }
      }

      val cachedValuesManager = CachedValuesManager.getManager(project)
      val files = cachedValuesManager.getCachedValue(project) {
        val proguardFiles = FileTypeIndex.getFiles(ProguardR8FileType.INSTANCE, GlobalSearchScope.projectScope(project))
        CachedValueProvider.Result(proguardFiles, VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS)
      }
      return if (files.isEmpty()) null else GlobalSearchScope.filesScope(project, files)
    }
    return null
  }
}