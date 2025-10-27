package de.maibornwolff.dependacharta.pipeline.analysis.model

import kotlinx.serialization.Serializable

@Serializable
data class NodeDependencies(
    val internalDependencies: Set<Dependency>,
    val externalDependencies: Set<Dependency>
) {
    operator fun plus(it: NodeDependencies): NodeDependencies =
        this.copy(
            internalDependencies = internalDependencies + it.internalDependencies,
            externalDependencies = externalDependencies + it.externalDependencies
        )
}
