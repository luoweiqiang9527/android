plugins {
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-kapt")
}

android {
  dataBinding {
    enabled = true
  }
}