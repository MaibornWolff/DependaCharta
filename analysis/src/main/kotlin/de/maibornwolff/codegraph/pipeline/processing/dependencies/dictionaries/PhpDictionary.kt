package de.maibornwolff.codegraph.pipeline.processing.dependencies.dictionaries

import de.maibornwolff.codegraph.pipeline.analysis.model.Path

class PhpDictionary : LanguageDictionary {
    override fun get() = phpDictionary

    companion object {
        private val phpKeywords: Map<String, String> = mapOf()
        private val phpTypes: Map<String, String> = mapOf()
        private val phpDictionary =
            (phpTypes + phpKeywords).mapValues { Path(it.value.split(".")) }
    }
}
