buildscript {
    repositories {
        jcenter()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.4.0")
    }
}

plugins {
  id("org.jetbrains.kotlin.android") version "1.3.20" apply false
}

allprojects {
    repositories {
        jcenter()
    }
}
