package de.maibornwolff.dependacharta.pipeline.processing.levelization.model

data class GraphEdge(
    val source: String,
    val target: String,
    val weight: Int,
    val type: String
) {
    fun id(): String = "$source-$target"
}
