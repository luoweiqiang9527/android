plugins {
  id("com.android.application")
  id("kotlin-android")
}

android {
  dataBinding {
    <warning descr="If you plan to use data binding in a Kotlin project, you should apply the kotlin-kapt plugin.">enabled = true</warning>
  }
}
