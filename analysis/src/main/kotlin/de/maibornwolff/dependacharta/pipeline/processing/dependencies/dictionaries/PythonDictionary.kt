package de.maibornwolff.dependacharta.pipeline.processing.dependencies.dictionaries

import de.maibornwolff.dependacharta.pipeline.analysis.model.Path

class PythonDictionary : LanguageDictionary {
    override fun get() = pythonDictionary

    companion object {
        private val pythonKeywords: Map<String, String> = mapOf()
        private val pythonTypes: Map<String, String> = mapOf()
        private val pythonDictionary =
            (pythonTypes + pythonKeywords).mapValues { Path(it.value.split(".")) }
    }
}
