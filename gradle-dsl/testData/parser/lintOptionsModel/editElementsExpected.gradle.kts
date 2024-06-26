android {
  lintOptions {
    isAbortOnError = false
    isAbsolutePaths = true
    baselineFile = file("other-baseline.xml")
    check("check-id-1", "check-id-3")
    isCheckAllWarnings = false
    isCheckDependencies = true
    isCheckGeneratedSources = false
    isCheckReleaseBuilds = true
    isCheckTestSources = false
    disable("disable-id-1", "disable-id-3")
    enable("enable-id-1", "enable-id-3")
    error("error-id-3", "error-id-2")
    isExplainIssues = false
    fatal("fatal-id-1", "fatal-id-3")
    htmlOutput = file("other-html.output")
    htmlReport = false
    ignore("ignore-id-1", "ignore-id-3")
    isIgnoreTestSources = true
    isIgnoreWarnings = false
    informational("informational-id-3", "informational-id-2")
    lintConfig = file("other-lint.config")
    isNoLines = true
    isQuiet = false
    sarifOutput = file("other-sarif.output")
    sarifReport = false
    isShowAll = true
    textOutput = file("other-text.output")
    textReport = false
    warning("warning-id-1", "warning-id-3")
    isWarningsAsErrors = true
    xmlOutput = file("other-xml.output")
    xmlReport = false
  }
}
