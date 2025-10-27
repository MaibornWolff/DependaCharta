package de.maibornwolff.codegraph.pipeline.processing.dependencies.dictionaries

import de.maibornwolff.codegraph.pipeline.analysis.model.Path

class CppDictionary : LanguageDictionary {
    override fun get(): Map<String, Path> = cppDictionary

    companion object {
        val cppPrimitiveTypes = mapOf(
            "int" to "int",
            "char" to "char",
            "void" to "void",
            "bool" to "bool",
            "float" to "float",
            "double" to "double",
            "short" to "short",
            "long" to "long",
            "shared_ptr" to "std::shared_ptr",
        )
    }

    private val cppDictionary = cppPrimitiveTypes.mapValues { Path(it.value.split(".")) }
}
