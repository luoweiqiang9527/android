load("//tools/adt/idea/android/integration:build_defs.bzl", "INTEGRATION_TEST_GRADLE_VERSION", "INTEGRATION_TEST_SYSTEM_IMAGE")
load("//tools/adt/idea/studio:studio.bzl", "iml_studio_test")
load("//tools/base/bazel:bazel.bzl", "iml_module")
load("//tools/base/bazel:maven.bzl", "maven_repository")

# managed by go/iml_to_build
iml_module(
    name = "app-inspection.integration",
    enable_tests = False,
    iml_files = ["app-inspection.integration.iml"],
    test_srcs = ["testSrc"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//prebuilts/tools/common/m2:junit-4.12[test]",
        "//tools/adt/idea/as-driver:as-driver.utils[module, test]",
        "//tools/base/testutils:studio.android.sdktools.testutils[module, test]",
        "//prebuilts/studio/intellij-sdk:studio-sdk[test]",
        "//tools/adt/idea/.idea/libraries:truth[test]",
    ],
)

iml_studio_test(
    name = "LaunchInspectorsTest",
    timeout = "long",
    data = [
        ":minapp",
        ":minapp_deps",
        "//prebuilts/studio/sdk:build-tools/30.0.3",
        "//prebuilts/studio/sdk:emulator",
        "//prebuilts/studio/sdk:platform-tools",
        INTEGRATION_TEST_GRADLE_VERSION,
        INTEGRATION_TEST_SYSTEM_IMAGE,
    ],
    module = ":app-inspection.integration",
    tags_linux = [
        "block-network",
    ],
    tags_mac = [],
    tags_windows = [
        "block-network",
        "manual",  # RBE environment doesn't allow emulation (requires nested virtualization).
    ],
    test_class = "com.android.tools.idea.appinspection.LaunchInspectorsTest",
)

maven_repository(
    name = "minapp_deps",
    # keep sorted: for buildifier
    artifacts = [
        "@maven//:com.android.tools.build.aapt2_7.1.0-7984345",
        "@maven//:com.android.tools.build.gradle_7.1.0",
        "@maven//:org.jetbrains.kotlin.kotlin-compiler_1.9.20-Beta",
        "@maven//:org.jetbrains.kotlin.kotlin-gradle-plugin-api_1.9.20-Beta",
        "@maven//:org.jetbrains.kotlin.kotlin-gradle-plugin_1.9.20-Beta",
        "@maven//:org.jetbrains.kotlin.kotlin-stdlib-jdk8_1.9.20-Beta",
        "@maven//:org.jetbrains.kotlinx.kotlinx-coroutines-core_1.4.1",
        "@maven//:org.jetbrains.markdown_0.2.1",
    ],
)

filegroup(
    name = "minapp",
    srcs = glob(["testData/minapp/**"]),
)
