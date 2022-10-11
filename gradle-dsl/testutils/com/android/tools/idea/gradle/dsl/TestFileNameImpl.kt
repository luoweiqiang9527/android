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
package com.android.tools.idea.gradle.dsl

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.TestDataFile
import com.intellij.testFramework.TestDataPath
import org.jetbrains.annotations.SystemIndependent
import java.io.File

interface TestFileName {
  fun toFile(basePath: @SystemIndependent String, extension: String): File = File(FileUtil.toSystemDependentName(basePath) + extension)
}
@TestDataPath("\$CONTENT_ROOT/../testData/parser")
enum class TestFileNameImpl(@TestDataFile val path: String): TestFileName {
  AAPT_OPTIONS_PARSE_ELEMENTS_ONE("aaptOptions/parseElementsOne"),
  AAPT_OPTIONS_PARSE_ELEMENTS_TWO("aaptOptions/parseElementsTwo"),
  AAPT_OPTIONS_EDIT_ELEMENTS("aaptOptions/editElements"),
  AAPT_OPTIONS_EDIT_ELEMENTS_EXPECTED("aaptOptions/editElementsExpected"),
  AAPT_OPTIONS_EDIT_IGNORE_ASSET_PATTERN("aaptOptions/editIgnoreAssetPattern"),
  AAPT_OPTIONS_EDIT_IGNORE_ASSET_PATTERN_EXPECTED("aaptOptions/editIgnoreAssetPatternExpected"),
  AAPT_OPTIONS_ADD_ELEMENTS("aaptOptions/addElements"),
  AAPT_OPTIONS_ADD_ELEMENTS_EXPECTED("aaptOptions/addElementsExpected"),
  AAPT_OPTIONS_REMOVE_ELEMENTS("aaptOptions/removeElements"),
  AAPT_OPTIONS_REMOVE_ONE_ELEMENT("aaptOptions/removeOneElementInList"),
  AAPT_OPTIONS_REMOVE_ONE_ELEMENT_EXPECTED("aaptOptions/removeOneElementInListExpected"),
  AAPT_OPTIONS_REMOVE_LAST_ELEMENT("aaptOptions/removeLastElementInList"),
  BUILD_FEATURES_MODEL_PARSE_ELEMENTS("buildFeaturesModel/parseElements"),
  BUILD_FEATURES_MODEL_EDIT_ELEMENTS("buildFeaturesModel/editElements"),
  BUILD_FEATURES_MODEL_EDIT_ELEMENTS_EXPECTED("buildFeaturesModel/editElementsExpected"),
  BUILD_FEATURES_MODEL_ADD_ELEMENTS("buildFeaturesModel/addElements"),
  BUILD_FEATURES_MODEL_ADD_ELEMENTS_EXPECTED("buildFeaturesModel/addElementsExpected"),
  BUILD_FEATURES_MODEL_ADD_ELEMENTS_FROM_EXISTING("buildFeaturesModel/addElementsFromExisting"),
  BUILD_FEATURES_MODEL_ADD_ELEMENTS_FROM_EXISTING_EXPECTED("buildFeaturesModel/addElementsFromExistingExpected"),
  BUILD_FEATURES_MODEL_REMOVE_ELEMENTS("buildFeaturesModel/removeElements"),
  COMPILE_OPTIONS_MODEL_COMPILE_OPTIONS_BLOCK("compileOptionsModel/compileOptionsBlock"),
  COMPILE_OPTIONS_MODEL_COMPILE_OPTIONS_BLOCK_USING_ASSIGNMENT("compileOptionsModel/compileOptionsBlockUsingAssignment"),
  COMPILE_OPTIONS_MODEL_COMPILE_OPTIONS_APPLICATION_STATEMENT("compileOptionsModel/compileOptionsApplicationStatement"),
  COMPILE_OPTIONS_MODEL_COMPILE_OPTIONS_BLOCK_WITH_OVERRIDE_STATEMENT("compileOptionsModel/compileOptionsBlockWithOverrideStatement"),
  COMPILE_OPTIONS_MODEL_COMPILE_OPTIONS_REMOVE_APPLICATION_STATEMENT("compileOptionsModel/compileOptionsRemoveApplicationStatement"),
  COMPILE_OPTIONS_MODEL_COMPILE_OPTIONS_REMOVE_APPLICATION_STATEMENT_EXPECTED("compileOptionsModel/compileOptionsRemoveApplicationStatementExpected"),
  COMPILE_OPTIONS_MODEL_COMPILE_OPTIONS_MODIFY("compileOptionsModel/compileOptionsModify"),
  COMPILE_OPTIONS_MODEL_COMPILE_OPTIONS_MODIFY_EXPECTED("compileOptionsModel/compileOptionsModifyExpected"),
  COMPILE_OPTIONS_MODEL_COMPILE_OPTIONS_MODIFY_LONG_IDENTIFIER("compileOptionsModel/compileOptionsModifyLongIdentifier"),
  COMPILE_OPTIONS_MODEL_COMPILE_OPTIONS_MODIFY_LONG_IDENTIFIER_EXPECTED("compileOptionsModel/compileOptionsModifyLongIdentifierExpected"),
  COMPILE_OPTIONS_MODEL_COMPILE_OPTIONS_ADD("compileOptionsModel/compileOptionsAdd"),
  COMPILE_OPTIONS_MODEL_COMPILE_OPTIONS_ADD_EXPECTED("compileOptionsModel/compileOptionsAddExpected"),
  DATA_BINDING_MODEL_PARSE_ELEMENTS("dataBindingModel/parseElements"),
  DATA_BINDING_MODEL_EDIT_ELEMENTS("dataBindingModel/editElements"),
  DATA_BINDING_MODEL_EDIT_ELEMENTS_EXPECTED("dataBindingModel/editElementsExpected"),
  DATA_BINDING_MODEL_ADD_ELEMENTS("dataBindingModel/addElements"),
  DATA_BINDING_MODEL_ADD_ELEMENTS_EXPECTED("dataBindingModel/addElementsExpected"),
  DATA_BINDING_MODEL_ADD_ELEMENTS_FROM_EXISTING("dataBindingModel/addElementsFromExisting"),
  DATA_BINDING_MODEL_ADD_ELEMENTS_FROM_EXISTING_EXPECTED("dataBindingModel/addElementsFromExistingExpected"),
  DATA_BINDING_MODEL_REMOVE_ELEMENTS("dataBindingModel/removeElements"),
  DEX_OPTIONS_MODEL_PARSE_ELEMENTS_IN_APPLICATION_STATEMENTS("dexOptionsModel/parseElementsInApplicationStatements"),
  DEX_OPTIONS_MODEL_PARSE_ELEMENTS_IN_ASSIGNMENT_STATEMENTS("dexOptionsModel/parseElementsInAssignmentStatements"),
  DEX_OPTIONS_MODEL_EDIT_ELEMENTS("dexOptionsModel/editElements"),
  DEX_OPTIONS_MODEL_EDIT_ELEMENTS_EXPECTED("dexOptionsModel/editElementsExpected"),
  DEX_OPTIONS_MODEL_ADD_ELEMENTS("dexOptionsModel/addElements"),
  DEX_OPTIONS_MODEL_ADD_ELEMENTS_EXPECTED("dexOptionsModel/addElementsExpected"),
  DEX_OPTIONS_MODEL_REMOVE_ELEMENTS("dexOptionsModel/removeElements"),
  DEX_OPTIONS_MODEL_REMOVE_ONE_OF_ELEMENTS_IN_THE_LIST("dexOptionsModel/removeOneOfElementsInTheList"),
  DEX_OPTIONS_MODEL_REMOVE_ONE_OF_ELEMENTS_IN_THE_LIST_EXPECTED("dexOptionsModel/removeOneOfElementsInTheListExpected"),
  DEX_OPTIONS_MODEL_REMOVE_ONLY_ELEMENT_IN_THE_LIST("dexOptionsModel/removeOnlyElementInTheList"),
  EXTERNAL_NATIVE_BUILD_MODEL_C_MAKE("externalNativeBuildModel/cMake"),
  EXTERNAL_NATIVE_BUILD_MODEL_C_MAKE_WITH_NEW_FILE_PATH("externalNativeBuildModel/cMakeWithNewFilePath"),
  EXTERNAL_NATIVE_BUILD_MODEL_C_MAKE_WITH_VERSION("externalNativeBuildModel/cMakeWithVersion"),
  EXTERNAL_NATIVE_BUILD_MODEL_REMOVE_C_MAKE_AND_RESET("externalNativeBuildModel/removeCMakeAndReset"),
  EXTERNAL_NATIVE_BUILD_MODEL_REMOVE_C_MAKE_AND_APPLY_CHANGES("externalNativeBuildModel/removeCMakeAndApplyChanges"),
  EXTERNAL_NATIVE_BUILD_MODEL_ADD_C_MAKE_PATH_AND_RESET("externalNativeBuildModel/addCMakePathAndReset"),
  EXTERNAL_NATIVE_BUILD_MODEL_ADD_C_MAKE_PATH_AND_APPLY_CHANGES("externalNativeBuildModel/addCMakePathAndApplyChanges"),
  EXTERNAL_NATIVE_BUILD_MODEL_ADD_C_MAKE_PATH_AND_APPLY_CHANGES_EXPECTED("externalNativeBuildModel/addCMakePathAndApplyChangesExpected"),
  EXTERNAL_NATIVE_BUILD_MODEL_ADD_C_MAKE_VERSION_AND_RESET("externalNativeBuildModel/addCMakeVersionAndReset"),
  EXTERNAL_NATIVE_BUILD_MODEL_ADD_C_MAKE_VERSION_AND_APPLY_CHANGES("externalNativeBuildModel/addCMakeVersionAndApplyChanges"),
  EXTERNAL_NATIVE_BUILD_MODEL_ADD_C_MAKE_VERSION_AND_APPLY_CHANGES_EXPECTED("externalNativeBuildModel/addCMakeVersionAndApplyChangesExpected"),
  EXTERNAL_NATIVE_BUILD_MODEL_NDK_BUILD("externalNativeBuildModel/ndkBuild"),
  EXTERNAL_NATIVE_BUILD_MODEL_NDK_BUILD_WITH_NEW_FILE_PATH("externalNativeBuildModel/ndkBuildWithNewFilePath"),
  EXTERNAL_NATIVE_BUILD_MODEL_NDK_BUILD_WITH_VERSION("externalNativeBuildModel/ndkBuildWithVersion"),
  EXTERNAL_NATIVE_BUILD_MODEL_REMOVE_NDK_BUILD_AND_RESET("externalNativeBuildModel/removeNdkBuildAndReset"),
  EXTERNAL_NATIVE_BUILD_MODEL_REMOVE_NDK_BUILD_AND_APPLY_CHANGES("externalNativeBuildModel/removeNdkBuildAndApplyChanges"),
  EXTERNAL_NATIVE_BUILD_MODEL_ADD_NDK_BUILD_PATH_AND_RESET("externalNativeBuildModel/addNdkBuildPathAndReset"),
  EXTERNAL_NATIVE_BUILD_MODEL_ADD_NDK_BUILD_PATH_AND_APPLY_CHANGES("externalNativeBuildModel/addNdkBuildPathAndApplyChanges"),
  EXTERNAL_NATIVE_BUILD_MODEL_ADD_NDK_BUILD_PATH_AND_APPLY_CHANGES_EXPECTED("externalNativeBuildModel/addNdkBuildPathAndApplyChangesExpected"),
  EXTERNAL_NATIVE_BUILD_MODEL_ADD_NDK_BUILD_VERSION_AND_RESET("externalNativeBuildModel/addNdkBuildVersionAndReset"),
  EXTERNAL_NATIVE_BUILD_MODEL_ADD_NDK_BUILD_VERSION_AND_APPLY_CHANGES("externalNativeBuildModel/addNdkBuildVersionAndApplyChanges"),
  EXTERNAL_NATIVE_BUILD_MODEL_ADD_NDK_BUILD_VERSION_AND_APPLY_CHANGES_EXPECTED("externalNativeBuildModel/addNdkBuildVersionAndApplyChangesExpected"),
  EXTERNAL_NATIVE_BUILD_MODEL_SET_CONSTRUCTOR_TO_FUNCTION("externalNativeBuildModel/setConstructorToFunction"),
  EXTERNAL_NATIVE_BUILD_MODEL_SET_CONSTRUCTOR_TO_FUNCTION_EXPECTED("externalNativeBuildModel/setConstructorToFunctionExpected"),
  MODEL_MAP_PROPERTY_IMPL_PROPERTY_VALUES("modelMapPropertyImpl/propertyValues"),
  PROPERTY_MODEL_UTILS_TEST_AS_FILE("propertyModelUtils/testAsFile"),
  SOURCE_DIRECTORY_MODEL_SOURCE_DIRECTORY_TEXT("sourceDirectoryModel/sourceDirectoryText"),
  SOURCE_DIRECTORY_MODEL_SOURCE_DIRECTORY_ENTRIES_ADD_AND_APPLY_EXPECTED("sourceDirectoryModel/sourceDirectoryEntriesAddAndApplyExpected"),
  SOURCE_DIRECTORY_MODEL_SOURCE_DIRECTORY_ENTRIES_REMOVE_AND_APPLY_EXPECTED("sourceDirectoryModel/sourceDirectoryEntriesRemoveAndApplyExpected"),
  SOURCE_DIRECTORY_MODEL_SOURCE_DIRECTORY_ENTRIES_REPLACE_AND_APPLY_EXPECTED("sourceDirectoryModel/sourceDirectoryEntriesReplaceAndApplyExpected"),
  SOURCE_FILE_MODEL_SOURCE_FILE("sourceFileModel/sourceFile"),
  SOURCE_FILE_MODEL_SOURCE_FILE_EDIT_AND_RESET("sourceFileModel/sourceFileEditAndReset"),
  SOURCE_FILE_MODEL_SOURCE_FILE_EDIT_AND_APPLY("sourceFileModel/sourceFileEditAndApply"),
  SOURCE_FILE_MODEL_SOURCE_FILE_EDIT_AND_APPLY_EXPECTED("sourceFileModel/sourceFileEditAndApplyExpected"),
  SOURCE_FILE_MODEL_SOURCE_FILE_ADD_AND_RESET("sourceFileModel/sourceFileAddAndReset"),
  SOURCE_FILE_MODEL_SOURCE_FILE_ADD_AND_APPLY("sourceFileModel/sourceFileAddAndApply"),
  SOURCE_FILE_MODEL_SOURCE_FILE_ADD_AND_APPLY_EXPECTED("sourceFileModel/sourceFileAddAndApplyExpected"),
  SOURCE_FILE_MODEL_SOURCE_FILE_REMOVE_AND_RESET("sourceFileModel/sourceFileRemoveAndReset"),
  SOURCE_FILE_MODEL_SOURCE_FILE_REMOVE_AND_APPLY("sourceFileModel/sourceFileRemoveAndApply"),
  SOURCE_SET_MODEL_SET_ROOT_IN_SOURCE_SET_BLOCK("sourceSetModel/setRootInSourceSetBlock"),
  SOURCE_SET_MODEL_SET_ROOT_STATEMENTS("sourceSetModel/setRootStatements"),
  SOURCE_SET_MODEL_SET_ROOT_OVERRIDE_STATEMENTS("sourceSetModel/setRootOverrideStatements"),
  SOURCE_SET_MODEL_SET_ROOT_EDIT_AND_RESET("sourceSetModel/setRootEditAndReset"),
  SOURCE_SET_MODEL_SET_ROOT_EDIT_AND_APPLY("sourceSetModel/setRootEditAndApply"),
  SOURCE_SET_MODEL_SET_ROOT_EDIT_AND_APPLY_EXPECTED("sourceSetModel/setRootEditAndApplyExpected"),
  SOURCE_SET_MODEL_SET_ROOT_ADD_AND_RESET("sourceSetModel/setRootAddAndReset"),
  SOURCE_SET_MODEL_SET_ROOT_ADD_AND_APPLY("sourceSetModel/setRootAddAndApply"),
  SOURCE_SET_MODEL_SET_ROOT_ADD_AND_APPLY_EXPECTED("sourceSetModel/setRootAddAndApplyExpected"),
  SOURCE_SET_MODEL_SET_ROOT_REMOVE_AND_RESET("sourceSetModel/setRootRemoveAndReset"),
  SOURCE_SET_MODEL_SET_ROOT_REMOVE_AND_APPLY("sourceSetModel/setRootRemoveAndApply"),
  SOURCE_SET_MODEL_ADD_AND_APPLY_BLOCK_ELEMENTS("sourceSetModel/addAndApplyBlockElements"),
  SOURCE_SET_MODEL_ADD_AND_APPLY_BLOCK_ELEMENTS_EXPECTED("sourceSetModel/addAndApplyBlockElementsExpected"),
  SOURCE_SET_MODEL_REMOVE_AND_APPLY_BLOCK_ELEMENTS("sourceSetModel/removeAndApplyBlockElements"),
  SPLITS_MODEL_ADD_ELEMENTS("splitsModel/addElements"),
  SPLITS_MODEL_ADD_ELEMENTS_EXPECTED("splitsModel/addElementsExpected"),
  SPLITS_MODEL_SPLITS_EDIT_ELEMENTS_EXPECTED("splitsModel/editElementsExpected"),
  SPLITS_MODEL_REMOVE_BLOCK_ELEMENTS("splitsModel/removeBlockElements"),
  SPLITS_MODEL_REMOVE_ONE_OF_ELEMENTS_IN_THE_LIST("splitsModel/removeOneOfElementsInTheList"),
  SPLITS_MODEL_REMOVE_ONE_OF_ELEMENTS_IN_THE_LIST_EXPECTED("splitsModel/removeOneOfElementsInTheListExpected"),
  SPLITS_MODEL_REMOVE_ONLY_ELEMENTS_IN_THE_LIST("splitsModel/removeOnlyElementsInTheList"),
  SPLITS_MODEL_RESET_STATEMENT("splitsModel/resetStatement"),
  SPLITS_MODEL_RESET_NONE_EXISTING("splitsModel/resetNoneExisting"),
  SPLITS_MODEL_RESET_AND_INITIALIZE("splitsModel/resetAndInitialize"),
  SPLITS_MODEL_ADD_RESET_STATEMENT("splitsModel/addResetStatement"),
  SPLITS_MODEL_ADD_RESET_STATEMENT_EXPECTED("splitsModel/addResetStatementExpected"),
  SPLITS_MODEL_REMOVE_RESET_STATEMENT("splitsModel/removeResetStatement"),
  SPLITS_MODEL_REMOVE_RESET_STATEMENT_EXPECTED("splitsModel/removeResetStatementExpected"),
  SPLITS_MODEL_SPLITS_TEXT("splitsModel/splitsText"),
  TEST_OPTIONS_MODEL_ADD_ELEMENTS("testOptionsModel/addElements"),
  TEST_OPTIONS_MODEL_ADD_ELEMENTS_EXPECTED("testOptionsModel/addElementsExpected"),
  TEST_OPTIONS_MODEL_TEST_OPTIONS_TEXT("testOptionsModel/testOptionsText"),
  TEST_OPTIONS_MODEL_EDIT_ELEMENTS_EXPECTED("testOptionsModel/editElementsExpected"),
  VIEW_BINDING_MODEL_PARSE_ELEMENTS("viewBindingModel/parseElements"),
  VIEW_BINDING_MODEL_EDIT_ELEMENTS("viewBindingModel/editElements"),
  VIEW_BINDING_MODEL_EDIT_ELEMENTS_EXPECTED("viewBindingModel/editElementsExpected"),
  VIEW_BINDING_MODEL_ADD_ELEMENTS("viewBindingModel/addElements"),
  VIEW_BINDING_MODEL_ADD_ELEMENTS_EXPECTED("viewBindingModel/addElementsExpected"),
  VIEW_BINDING_MODEL_ADD_ELEMENTS_FROM_EXISTING("viewBindingModel/addElementsFromExisting"),
  VIEW_BINDING_MODEL_ADD_ELEMENTS_FROM_EXISTING_EXPECTED("viewBindingModel/addElementsFromExistingExpected"),
  VIEW_BINDING_MODEL_REMOVE_ELEMENTS("viewBindingModel/removeElements"),
  BUILD_SCRIPT_MODEL_PARSE_DEPENDENCIES("buildScriptModel/parseDependencies"),
  BUILD_SCRIPT_MODEL_ADD_DEPENDENCY("buildScriptModel/addDependency"),
  BUILD_SCRIPT_MODEL_ADD_DEPENDENCY_EXPECTED("buildScriptModel/addDependencyExpected"),
  BUILD_SCRIPT_MODEL_EDIT_DEPENDENCY("buildScriptModel/editDependency"),
  BUILD_SCRIPT_MODEL_EDIT_DEPENDENCY_EXPECTED("buildScriptModel/editDependencyExpected"),
  BUILD_SCRIPT_MODEL_PARSE_REPOSITORIES("buildScriptModel/parseRepositories"),
  BUILD_SCRIPT_MODEL_REMOVE_REPOSITORIES_SINGLE_BLOCK("buildScriptModel/removeRepositoriesSingleBlock"),
  BUILD_SCRIPT_MODEL_REMOVE_REPOSITORIES_MULTIPLE_BLOCKS("buildScriptModel/removeRepositoriesMultipleBlocks"),
  BUILD_SCRIPT_MODEL_EXT_PROPERTIES_FROM_BUILDSCRIPT_BLOCK("buildScriptModel/extPropertiesFromBuildscriptBlock"),
  BUILD_SCRIPT_MODEL_EXT_PROPERTIES_FROM_BUILDSCRIPT_BLOCK_SUB("buildScriptModel/extPropertiesFromBuildscriptBlock_sub"),
  BUILD_SCRIPT_MODEL_EXT_PROPERTIES_NOT_VISIBLE_FROM_BUILDSCRIPT_BLOCK("buildScriptModel/extPropertiesNotVisibleFromBuildscriptBlock"),
  BUILD_SCRIPT_MODEL_EXT_PROPERTIES_NOT_VISIBLE_FROM_BUILDSCRIPT_BLOCK_EXPECTED("buildScriptModel/extPropertiesNotVisibleFromBuildscriptBlockExpected"),
  REFERENCE_RESOLUTION_RESOLVE_OTHER_PROJECT_PATH("referenceResolution/resolveOtherProjectPath"),
  REFERENCE_RESOLUTION_RESOLVE_OTHER_PROJECT_PATH_SUB("referenceResolution/resolveOtherProjectPath_sub"),
  REFERENCE_RESOLUTION_RESOLVE_PARENT("referenceResolution/resolveParent"),
  REFERENCE_RESOLUTION_RESOLVE_PARENT_SUB("referenceResolution/resolveParent_sub"),
  REFERENCE_RESOLUTION_RESOLVE_PROJECT("referenceResolution/resolveProject"),
  REFERENCE_RESOLUTION_RESOLVE_PROJECT_PATH_SUB("referenceResolution/resolveProjectPath_sub"),
  REFERENCE_RESOLUTION_RESOLVE_PROJECT_DIR_SUB("referenceResolution/resolveProjectDir_sub"),
  REFERENCE_RESOLUTION_RESOLVE_ROOT_DIR_SUB("referenceResolution/resolveRootDir_sub"),
  REFERENCE_RESOLUTION_RESOLVE_ROOT_PROJECT("referenceResolution/resolveRootProject"),
  REFERENCE_RESOLUTION_RESOLVE_ROOT_PROJECT_SUB("referenceResolution/resolveRootProject_sub"),
  DEPENDENCIES_ALL_DEPENDENCIES("dependencies/allDependencies"),
  DEPENDENCIES_KOTLIN_DEPENDENCIES("dependencies/kotlinDependencies"),
  DEPENDENCIES_REMOVE_JAR_DEPENDENCIES("dependencies/removeJarDependencies"),
  DEPENDENCIES_NON_IDENTIFIER_CONFIGURATION("dependencies/nonIdentifierConfiguration"),
  DEPENDENCIES_ADD_NON_IDENTIFIER_CONFIGURATION_EXPECTED("dependencies/addNonIdentifierConfigurationExpected"),
  DEPENDENCIES_SET_NON_IDENTIFIER_CONFIGURATION_EXPECTED("dependencies/setNonIdentifierConfigurationExpected"),
  FILE_DEPENDENCY_INSERTION_ORDER("fileDependency/insertionOrder"),
  FILE_DEPENDENCY_INSERTION_ORDER_EXPECTED("fileDependency/insertionOrderExpected"),
  FILE_DEPENDENCY_PARSE_SINGLE_FILE_DEPENDENCY("fileDependency/parseSingleFileDependency"),
  FILE_DEPENDENCY_PARSE_MULTIPLE_FILE_DEPENDENCIES("fileDependency/parseMultipleFileDependencies"),
  FILE_DEPENDENCY_PARSE_FILE_DEPENDENCIES_WITH_CLOSURE("fileDependency/parseFileDependenciesWithClosure"),
  FILE_DEPENDENCY_SET_CONFIGURATION_WHEN_SINGLE("fileDependency/setConfigurationWhenSingle"),
  FILE_DEPENDENCY_SET_CONFIGURATION_WHEN_SINGLE_EXPECTED("fileDependency/setConfigurationWhenSingleExpected"),
  FILE_DEPENDENCY_SET_CONFIGURATION_WHEN_MULTIPLE("fileDependency/setConfigurationWhenMultiple"),
  FILE_DEPENDENCY_SET_FILE("fileDependency/setFile"),
  FILE_DEPENDENCY_SET_FILE_EXPECTED("fileDependency/setFileExpected"),
  FILE_DEPENDENCY_UPDATE_SOME_OF_FILE_DEPENDENCIES("fileDependency/updateSomeOfFileDependencies"),
  FILE_DEPENDENCY_UPDATE_SOME_OF_FILE_DEPENDENCIES_EXPECTED("fileDependency/updateSomeOfFileDependenciesExpected"),
  FILE_DEPENDENCY_ADD_FILE_DEPENDENCY("fileDependency/addFileDependency"),
  FILE_DEPENDENCY_ADD_FILE_DEPENDENCY_EXPECTED("fileDependency/addFileDependencyExpected"),
  FILE_DEPENDENCY_REMOVE_FILE_DEPENDENCY("fileDependency/removeFileDependency"),
  FILE_DEPENDENCY_REMOVE_ONE_OF_FILE_DEPENDENCY("fileDependency/removeOneOfFileDependency"),
  FILE_DEPENDENCY_REMOVE_ONE_OF_FILE_DEPENDENCY_EXPECTED("fileDependency/removeOneOfFileDependencyExpected"),
  FILE_DEPENDENCY_REMOVE_WHEN_MULTIPLE("fileDependency/removeWhenMultiple"),
  FILE_TREE_DEPENDENCY_PARSE_FILE_TREE_WITH_DIR_AND_INCLUDE_ATTRIBUTE_LIST("fileTreeDependency/parseFileTreeWithDirAndIncludeAttributeList"),
  FILE_TREE_DEPENDENCY_PARSE_FILE_TREE_WITH_DIR_AND_INCLUDE_ATTRIBUTE_PATTERN("fileTreeDependency/parseFileTreeWithDirAndIncludeAttributePattern"),
  FILE_TREE_DEPENDENCY_PARSE_FILE_TREE_WITH_DIR_AND_EXCLUDE_ATTRIBUTE_LIST("fileTreeDependency/parseFileTreeWithDirAndExcludeAttributeList"),
  FILE_TREE_DEPENDENCY_PARSE_FILE_TREE_WITH_DIR_ONLY("fileTreeDependency/parseFileTreeWithDirOnly"),
  FILE_TREE_DEPENDENCY_PARSE_FILE_TREE_MIXED("fileTreeDependency/parseFileTreeMixed"),
  FILE_TREE_DEPENDENCY_SET_DIR_WHEN_INCLUDE_SPECIFIED("fileTreeDependency/setDirWhenIncludeSpecified"),
  FILE_TREE_DEPENDENCY_SET_DIR_WHEN_INCLUDE_SPECIFIED_EXPECTED("fileTreeDependency/setDirWhenIncludeSpecifiedExpected"),
  FILE_TREE_DEPENDENCY_SET_DIR("fileTreeDependency/setDir"),
  FILE_TREE_DEPENDENCY_SET_DIR_EXPECTED("fileTreeDependency/setDirExpected"),
  FILE_TREE_DEPENDENCY_ADD_FILE_TREE_WITH_DIR_ONLY("fileTreeDependency/addFileTreeWithDirOnly"),
  FILE_TREE_DEPENDENCY_ADD_FILE_TREE_WITH_DIR_ONLY_EXPECTED("fileTreeDependency/addFileTreeWithDirOnlyExpected"),
  FILE_TREE_DEPENDENCY_ADD_FILE_TREE_WITH_DIR_AND_INCLUDE_ATTRIBUTE_PATTERN("fileTreeDependency/addFileTreeWithDirAndIncludeAttributePattern"),
  FILE_TREE_DEPENDENCY_ADD_FILE_TREE_WITH_DIR_AND_INCLUDE_ATTRIBUTE_PATTERN_EXPECTED("fileTreeDependency/addFileTreeWithDirAndIncludeAttributePatternExpected"),
  FILE_TREE_DEPENDENCY_ADD_FILE_TREE_WITH_DIR_AND_INCLUDE_ATTRIBUTE_LIST("fileTreeDependency/addFileTreeWithDirAndIncludeAttributeList"),
  FILE_TREE_DEPENDENCY_ADD_FILE_TREE_WITH_DIR_AND_INCLUDE_ATTRIBUTE_LIST_EXPECTED("fileTreeDependency/addFileTreeWithDirAndIncludeAttributeListExpected"),
  FILE_TREE_DEPENDENCY_ADD_FILE_TREE_WITH_DIR_AND_EXCLUDE_ATTRIBUTE_LIST("fileTreeDependency/addFileTreeWithDirAndExcludeAttributeList"),
  FILE_TREE_DEPENDENCY_ADD_FILE_TREE_WITH_DIR_AND_EXCLUDE_ATTRIBUTE_LIST_EXPECTED("fileTreeDependency/addFileTreeWithDirAndExcludeAttributeListExpected"),
  FILE_TREE_DEPENDENCY_REMOVE_FILE_TREE_DEPENDENCY("fileTreeDependency/removeFileTreeDependency"),
  FILE_TREE_DEPENDENCY_REMOVE_WHEN_MULTIPLE("fileTreeDependency/removeWhenMultiple"),
  FILE_TREE_DEPENDENCY_REMOVE_WHEN_MULTIPLE_EXPECTED("fileTreeDependency/removeWhenMultipleExpected"),
  FILE_TREE_DEPENDENCY_SET_CONFIGURATION_WHEN_SINGLE("fileTreeDependency/setConfigurationWhenSingle"),
  FILE_TREE_DEPENDENCY_SET_CONFIGURATION_WHEN_SINGLE_EXPECTED("fileTreeDependency/setConfigurationWhenSingleExpected"),
  FILE_TREE_DEPENDENCY_SET_CONFIGURATION_WHEN_MULTIPLE("fileTreeDependency/setConfigurationWhenMultiple"),
  FILE_TREE_DEPENDENCY_SET_DIR_FROM_EMPTY("fileTreeDependency/setDirFromEmpty"),
  FILE_TREE_DEPENDENCY_SET_DIR_WHEN_MULTIPLE("fileTreeDependency/setDirWhenMultiple"),
  FILE_TREE_DEPENDENCY_SET_DIR_WHEN_MULTIPLE_EXPECTED("fileTreeDependency/setDirWhenMultipleExpected"),
  FILE_TREE_DEPENDENCY_SET_REFERENCE_DIR_IN_METHOD_CALL_NOTATION("fileTreeDependency/setReferenceDirInMethodCallNotation"),
  FILE_TREE_DEPENDENCY_SET_REFERENCE_DIR_IN_METHOD_CALL_NOTATION_EXPECTED("fileTreeDependency/setReferenceDirInMethodCallNotationExpected"),
  FILE_TREE_DEPENDENCY_SET_INCLUDES_IN_METHOD_CALL_NOTATION("fileTreeDependency/setIncludesInMethodCallNotation"),
  FILE_TREE_DEPENDENCY_SET_INCLUDES_IN_METHOD_CALL_NOTATION_EXPECTED("fileTreeDependency/setIncludesInMethodCallNotationExpected"),
  FILE_TREE_DEPENDENCY_ADD_AND_REMOVE_INCLUDE_WITHOUT_APPLY("fileTreeDependency/addAndRemoveIncludeWithoutApply"),
  FILE_TREE_DEPENDENCY_REMOVE_ONLY_POSSIBLE_IN_MAP_FORM("fileTreeDependency/removeOnlyPossibleInMapForm"),
  BUILD_NOTIFICATION_INCOMPLETE_PARSING_NOTIFICATION("buildNotification/incompleteParsingNotification"),
  BUILD_NOTIFICATION_INCOMPLETE_PARSING_NOTIFICATION_IN_SUBMODULE_BUILD("buildNotification/incompleteParsingNotificationInSubmoduleBuild"),
  BUILD_NOTIFICATION_INCOMPLETE_PARSING_NOTIFICATION_IN_SUBMODULE_SUB("buildNotification/incompleteParsingNotificationInSubmoduleSub"),
  BUILD_NOTIFICATION_PROPERTY_PLACEMENT_NOTIFICATION("buildNotification/propertyPlacementNotification"),
  BUILD_NOTIFICATION_NO_PROPERTY_PLACEMENT_NOTIFICATION("buildNotification/noPropertyPlacementNotification"),
  BUILD_NOTIFICATION_NO_PROPERTY_PLACEMENT_NOTIFICATION_EXPECTED("buildNotification/noPropertyPlacementNotificationExpected"),
  EXT_MODEL_PARSING_SIMPLE_PROPERTY_PER_LINE("extModel/parsingSimplePropertyPerLine"),
  EXT_MODEL_PARSING_SIMPLE_PROPERTY_IN_EXT_BLOCK("extModel/parsingSimplePropertyInExtBlock"),
  EXT_MODEL_PARSING_LIST_OF_PROPERTIES("extModel/parsingListOfProperties"),
  EXT_MODEL_RESOLVE_EXT_PROPERTY("extModel/resolveExtProperty"),
  EXT_MODEL_RESOLVE_QUALIFIED_EXT_PROPERTY("extModel/resolveQualifiedExtProperty"),
  EXT_MODEL_RESOLVE_MULTI_LEVEL_EXT_PROPERTY("extModel/resolveMultiLevelExtProperty"),
  EXT_MODEL_RESOLVE_VARIABLES_IN_STRING_LITERAL("extModel/resolveVariablesInStringLiteral"),
  EXT_MODEL_RESOLVE_QUALIFIED_VARIABLE_IN_STRING_LITERAL("extModel/resolveQualifiedVariableInStringLiteral"),
  EXT_MODEL_STRING_REFERENCE_IN_LIST_PROPERTY("extModel/stringReferenceInListProperty"),
  EXT_MODEL_LIST_REFERENCE_IN_LIST_PROPERTY("extModel/listReferenceInListProperty"),
  EXT_MODEL_RESOLVE_VARIABLE_IN_LIST_PROPERTY("extModel/resolveVariableInListProperty"),
  EXT_MODEL_STRING_REFERENCE_IN_MAP_PROPERTY("extModel/stringReferenceInMapProperty"),
  EXT_MODEL_MAP_REFERENCE_IN_MAP_PROPERTY("extModel/mapReferenceInMapProperty"),
  EXT_MODEL_RESOLVE_VARIABLE_IN_MAP_PROPERTY("extModel/resolveVariableInMapProperty"),
  EXT_MODEL_RESOLVE_MULTI_LEVEL_EXT_PROPERTY_WITH_HISTORY("extModel/resolveMultiLevelExtPropertyWithHistory"),
  EXT_MODEL_FLAT_DEF_VARIABLES_ARE_RESOLVED("extModel/flatDefVariablesAreResolved"),
  EXT_MODEL_NESTED_DEF_VARIABLES_ARE_RESOLVED("extModel/nestedDefVariablesAreResolved"),
  EXT_MODEL_MULTIPLE_DEF_DECLARATIONS("extModel/multipleDefDeclarations"),
  EXT_MODEL_DEF_USED_IN_DEF_RESOLVED("extModel/defUsedInDefResolved"),
  EXT_MODEL_DEPENDENCY_EXT_USAGE("extModel/dependencyExtUsage"),
  EXT_MODEL_BUILD_SCRIPT_EXT_USAGE("extModel/buildScriptExtUsage"),
  EXT_MODEL_MULTIPLE_EXT_BLOCKS("extModel/multipleExtBlocks"),
  EXT_MODEL_MULTIPLE_EXT_BLOCKS_EXPECTED("extModel/multipleExtBlocksExpected"),
  EXT_MODEL_EXT_FLAT_AND_BLOCK("extModel/extFlatAndBlock"),
  EXT_MODEL_EXT_FLAT_AND_BLOCK_EXPECTED("extModel/extFlatAndBlockExpected"),
  EXT_MODEL_PROPERTY_NAMES("extModel/propertyNames"),
  EXT_MODEL_REPLACE_CIRCULAR_REFERENCE("extModel/replaceCircularReference"),
  EXT_MODEL_RESOLVE_MULTI_MODULE_EXT_PROPERTY("extModel/resolveMultiModuleExtProperty"),
  EXT_MODEL_RESOLVE_MULTI_MODULE_EXT_PROPERTY_SUB("extModel/resolveMultiModuleExtProperty_sub"),
  EXT_MODEL_RESOLVE_VARIABLE_IN_SUBMODULE_BUILD_FILE("extModel/resolveVariableInSubModuleBuildFile"),
  EXT_MODEL_RESOLVE_VARIABLE_IN_SUBMODULE_BUILD_FILE_SUB("extModel/resolveVariableInSubModuleBuildFile_sub"),
  EXT_MODEL_RESOLVE_VARIABLE_IN_SUBMODULE_PROPERTIES_FILE("extModel/resolveVariableInSubModulePropertiesFile"),
  EXT_MODEL_RESOLVE_VARIABLE_IN_SUBMODULE_PROPERTIES_FILE_SUB("extModel/resolveVariableInSubModulePropertiesFile_sub"),
  EXT_MODEL_RESOLVE_VARIABLE_IN_MAINMODULE_PROPERTIES_FILE("extModel/resolveVariableInMainModulePropertiesFile"),
  EXT_MODEL_RESOLVE_VARIABLE_IN_MAINMODULE_PROPERTIES_FILE_SUB("extModel/resolveVariableInMainModulePropertiesFile_sub"),
  EXT_MODEL_RESOLVE_VARIABLE_IN_MAINMODULE_BUILD_FILE("extModel/resolveVariableInMainModuleBuildFile"),
  EXT_MODEL_RESOLVE_VARIABLE_IN_MAINMODULE_BUILD_FILE_SUB("extModel/resolveVariableInMainModuleBuildFile_sub"),
  EXT_MODEL_RESOLVE_MULTI_MODULE_EXT_PROPERTY_WITH_HISTORY("extModel/resolveMultiModuleExtPropertyWithHistory"),
  EXT_MODEL_RESOLVE_MULTI_MODULE_EXT_PROPERTY_WITH_HISTORY_SUB("extModel/resolveMultiModuleExtPropertyWithHistory_sub"),
  EXT_MODEL_RESOLVE_MULTI_MODULE_EXT_PROPERTY_FROM_PROPERTIES_WITH_HISTORY("extModel/resolveMultiModuleExtPropertyFromPropertiesWithHistory"),
  EXT_MODEL_RESOLVE_MULTI_MODULE_EXT_PROPERTY_FROM_PROPERTIES_WITH_HISTORY_SUB("extModel/resolveMultiModuleExtPropertyFromPropertiesWithHistory_sub"),
  PROPERTY_MODIFIED_TEST_FILE("propertyModified/testFile"),
  PROPERTY_MODIFIED_TEST_FILE2("propertyModified/testFile2"),
  PROPERTY_UTIL_WRITE_BACK_ELEMENT_WITH_TRIMMED_NAME("propertyUtil/writeBackElementWithTrimmedName"),
  GOOGLE_MAVEN_REPOSITORY_HAS_GOOGLE_MAVEN_REPOSITORY_EMPTY("googleMavenRepository/hasGoogleMavenRepositoryEmpty"),
  GOOGLE_MAVEN_REPOSITORY_HAS_GOOGLE_MAVEN_REPOSITORY_NAME3DOT5("googleMavenRepository/hasGoogleMavenRepositoryName3dot5"),
  GOOGLE_MAVEN_REPOSITORY_HAS_GOOGLE_MAVEN_REPOSITORY_NAME4DOT0("googleMavenRepository/hasGoogleMavenRepositoryName4dot0"),
  GOOGLE_MAVEN_REPOSITORY_HAS_GOOGLE_MAVEN_REPOSITORY_URL3DOT5("googleMavenRepository/hasGoogleMavenRepositoryUrl3dot5"),
  GOOGLE_MAVEN_REPOSITORY_HAS_GOOGLE_MAVEN_REPOSITORY_URL4DOT0("googleMavenRepository/hasGoogleMavenRepositoryUrl4dot0"),
  GOOGLE_MAVEN_REPOSITORY_ADD_GOOGLE_REPOSITORY_EMPTY4DOT0("googleMavenRepository/addGoogleRepositoryEmpty4dot0"),
  GOOGLE_MAVEN_REPOSITORY_ADD_GOOGLE_REPOSITORY_WITH_GOOGLE_ALREADY4DOT0("googleMavenRepository/addGoogleRepositoryWithGoogleAlready4dot0"),
  COMPOSITE_BUILD_COMPOSITE_PROJECT_APPLIED("compositeBuild/compositeProjectApplied"),
  COMPOSITE_BUILD_COMPOSITE_PROJECT_ROOT_BUILD("compositeBuild/compositeProjectRootBuild"),
  COMPOSITE_BUILD_COMPOSITE_PROJECT_SETTINGS("compositeBuild/compositeProjectSettings"),
  COMPOSITE_BUILD_COMPOSITE_PROJECT_SUB_MODULE_BUILD("compositeBuild/compositeProjectSubModuleBuild"),
  COMPOSITE_BUILD_MAIN_PROJECT_APPLIED("compositeBuild/mainProjectApplied"),
  COMPOSITE_BUILD_MAIN_PROJECT_ROOT_BUILD("compositeBuild/mainProjectRootBuild"),
  COMPOSITE_BUILD_MAIN_PROJECT_SETTINGS("compositeBuild/mainProjectSettings"),
  COMPOSITE_BUILD_MAIN_PROJECT_SUB_MODULE_BUILD("compositeBuild/mainProjectSubModuleBuild"),

  ;

  override fun toFile(basePath: String, extension: String): File = super.toFile("$basePath/$path", extension)
}
