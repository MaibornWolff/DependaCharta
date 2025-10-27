package de.maibornwolff.dependacharta.pipeline.processing.dependencies.dictionaries

import de.maibornwolff.dependacharta.pipeline.analysis.model.Path

class TypescriptDictionary : LanguageDictionary {
    override fun get() = typescriptDictionary

    companion object {
        private val typescriptKeywords: Map<String, String> = mapOf()
        private val typescriptTypes: Map<String, String> = mapOf()
        private val typescriptDictionary =
            (typescriptTypes + typescriptKeywords).mapValues { Path(it.value.split(".")) }
    }
}