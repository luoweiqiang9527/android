pluginManagement {
  val agpVersion = "3.5.0"
  repositories {
    google()
  }
  plugins {
    id("com.android.application") version "$agpVersion"
    id("com.android.library") version "$agpVersion"
  }
}
