plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "dependacharta-analysis"

// TEMPORARY: composite-build wiring to local TSE for the Python migration iteration phase.
// REVERT before opening the DC PR — see plans/add-python-dependency-support.md Task 7.
includeBuild("../../TreeSitterExcavationSite")
