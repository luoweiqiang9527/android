android {
  testOptions {
    emulatorSnapshots {
      enableForTestFailures = true
      maxSnapshotsForTestFailures = 100
    }
  }
}
