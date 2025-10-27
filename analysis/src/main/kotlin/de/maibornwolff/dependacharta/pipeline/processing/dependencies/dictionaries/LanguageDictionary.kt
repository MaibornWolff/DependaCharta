package de.maibornwolff.codegraph.pipeline.processing.dependencies.dictionaries

import de.maibornwolff.codegraph.pipeline.analysis.model.Path

interface LanguageDictionary {
    fun get(): Map<String, Path>
}