android {
  lintOptions {
    isAbortOnError = true
    isAbsolutePaths = false
    baselineFile = file("baseline.xml")
    check("check-id-1", "check-id-2")
    isCheckAllWarnings = true
    isCheckDependencies = false
    isCheckGeneratedSources = true
    isCheckReleaseBuilds = false
    isCheckTestSources = true
    disable("disable-id-1", "disable-id-2")
    enable("enable-id-1", "enable-id-2")
    error("error-id-1", "error-id-2")
    isExplainIssues = true
    fatal("fatal-id-1", "fatal-id-2")
    htmlOutput = file("html.output")
    htmlReport = false
    ignore("ignore-id-1", "ignore-id-2")
    isIgnoreTestSources = false
    isIgnoreWarnings = true
    informational("informational-id-1", "informational-id-2")
    lintConfig = file("lint.config")
    isNoLines = false
    isQuiet = true
    sarifOutput = file("sarif.output")
    sarifReport = true
    isShowAll = false
    textOutput = file("text.output")
    textReport = true
    warning("warning-id-1", "warning-id-2")
    isWarningsAsErrors = false
    xmlOutput = file("xml.output")
    xmlReport = true
  }
}
