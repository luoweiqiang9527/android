plugins {
    id("com.android.application")
}

dependencies {
    compile(files("my/libs/http1.jar"))
    //noinspection GradlePath,GradleDependency
    compile(files("my\\libs\\http2.jar"))
}
