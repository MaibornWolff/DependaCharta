package de.maibornwolff.dependacharta.pipeline.processing.dependencies.dictionaries

import de.maibornwolff.dependacharta.pipeline.analysis.model.Path

interface LanguageDictionary {
    fun get(): Map<String, Path>
}