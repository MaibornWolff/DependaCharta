package de.maibornwolff.dependacharta.pipeline.processing.dependencies.dictionaries

import de.maibornwolff.dependacharta.pipeline.analysis.model.Path

class EmptyStandardLibrary : StandardLibrary {
    override fun get(): Map<String, Path> {
        return emptyMap()
    }
}