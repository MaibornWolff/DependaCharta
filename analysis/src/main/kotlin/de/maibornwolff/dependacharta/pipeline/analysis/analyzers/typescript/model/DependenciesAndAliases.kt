package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.model

import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type

data class DependenciesAndAliases(
    val dependencies: Set<Dependency>,
    val importByAlias: Map<String, String>,
    val usedTypes: Set<Type> = emptySet(),
) {
    fun getAllKnownIdentifiers(): Set<String> =
        importByAlias.keys + dependencies
            .filter { it.isWildcard.not() }
            .map { dependency -> dependency.path.parts.last() }

    fun plus(other: DependenciesAndAliases): DependenciesAndAliases {
        return DependenciesAndAliases(
            dependencies = dependencies + other.dependencies,
            importByAlias = importByAlias + other.importByAlias,
            usedTypes = usedTypes + other.usedTypes
        )
    }
}