android {
  packagingOptions {
    merge("abc")
    pickFirst("foo.so")
    doNotStrip("bar.so")
    exclude("def")
  }
}
