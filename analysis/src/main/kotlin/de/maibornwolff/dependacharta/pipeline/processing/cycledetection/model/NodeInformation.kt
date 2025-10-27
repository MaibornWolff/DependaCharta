package de.maibornwolff.codegraph.pipeline.processing.cycledetection.model

data class NodeInformation(
    val id: String,
    val dependencies: Set<String>,
) {
    companion object
}
