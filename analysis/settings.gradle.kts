plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "dependacharta-analysis"

// Composite build: use local TSE checkout for development. REVERT BEFORE MERGING.
includeBuild("../../TreeSitterExcavationSite")
