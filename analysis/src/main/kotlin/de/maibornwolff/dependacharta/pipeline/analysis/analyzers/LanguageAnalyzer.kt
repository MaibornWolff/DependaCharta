package de.maibornwolff.dependacharta.pipeline.analysis.analyzers

import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport

interface LanguageAnalyzer {
    fun analyze(): FileReport
}
