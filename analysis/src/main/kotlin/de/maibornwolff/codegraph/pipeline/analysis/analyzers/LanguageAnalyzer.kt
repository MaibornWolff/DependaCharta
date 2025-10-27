package de.maibornwolff.codegraph.pipeline.analysis.analyzers

import de.maibornwolff.codegraph.pipeline.analysis.model.FileReport

interface LanguageAnalyzer {
    fun analyze(): FileReport
}
