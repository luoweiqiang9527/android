buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    <error>classpath("com.android.tools.build:gradle:0.1.0")</error>
  }
}

allprojects {
  repositories {
    jcenter()
  }
}
