load("//tools/base/bazel:bazel.bzl", "iml_module")

# managed by go/iml_to_build
iml_module(
    name = "intellij.android.app-quality-insights.play-vitals.view",
    srcs = ["src"],
    iml_files = ["intellij.android.app-quality-insights.play-vitals.view.iml"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//prebuilts/studio/intellij-sdk:studio-sdk",
        "//tools/adt/idea/artwork:intellij.android.artwork[module]",
        "//tools/base/flags:studio.android.sdktools.flags[module]",
        "//tools/adt/idea/android-common:intellij.android.common[module]",
        "//tools/adt/idea/app-quality-insights/api:intellij.android.app-quality-insights.api[module]",
        "//tools/adt/idea/app-quality-insights/ui:intellij.android.app-quality-insights.ui[module]",
        "//tools/adt/idea/adt-ui:intellij.android.adt.ui[module]",
        "//tools/adt/idea/app-quality-insights/play-vitals/model:intellij.android.app-quality-insights.play-vitals.model[module]",
        "//tools/adt/idea/.idea/libraries:studio-analytics-proto",
        "//tools/base/sdklib:studio.android.sdktools.sdklib[module]",
    ],
)

# managed by go/iml_to_build
iml_module(
    name = "intellij.android.app-quality-insights.play-vitals.view.tests",
    iml_files = ["intellij.android.app-quality-insights.play-vitals.view.tests.iml"],
    test_class = "com.android.tools.idea.vitals.ui.VitalsUiTestSuite",
    test_srcs = ["testSrc"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//prebuilts/studio/intellij-sdk:studio-sdk[test]",
        "//tools/adt/idea/.idea/libraries:truth[test]",
        "//prebuilts/studio/intellij-sdk:studio-sdk-test-framework[test]",
        "//tools/adt/idea/.idea/libraries:jetbrains.kotlinx.coroutines.test[test]",
        "//tools/adt/idea/adt-testutils:intellij.android.adt.testutils[module, test]",
        "//tools/base/testutils:studio.android.sdktools.testutils[module, test]",
        "//tools/adt/idea/android-test-framework:intellij.android.testFramework[module, test]",
        "//tools/adt/idea/app-quality-insights/play-vitals/view:intellij.android.app-quality-insights.play-vitals.view[module, test]",
        "//tools/adt/idea/app-quality-insights/api:intellij.android.app-quality-insights.api[module, test]",
        "//tools/adt/idea/adt-ui:intellij.android.adt.ui[module, test]",
        "//tools/adt/idea/app-quality-insights/play-vitals/model:intellij.android.app-quality-insights.play-vitals.model[module, test]",
        "//tools/adt/idea/app-quality-insights/ui:intellij.android.app-quality-insights.ui[module, test]",
        "//tools/adt/idea/android-common:intellij.android.common[module, test]",
        "//tools/adt/idea/artwork:intellij.android.artwork[module, test]",
        "//tools/adt/idea/app-quality-insights/ui:intellij.android.app-quality-insights.ui.tests[module, test]",
    ],
)
