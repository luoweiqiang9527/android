plugins {
    id("com.android.application")
}

android {
    compileSdkVersion(28)
}

dependencies {
    <warning descr="`testCompile` is deprecated; replace with `testImplementation`">testCompile</warning>("androidx.appcompat:appcompat:1.0.0")
}
