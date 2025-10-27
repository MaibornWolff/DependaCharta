package de.maibornwolff.dependacharta.pipeline.processing.cycledetection.model

data class NodeInformation(
    val id: String,
    val dependencies: Set<String>,
) {
    companion object
}
