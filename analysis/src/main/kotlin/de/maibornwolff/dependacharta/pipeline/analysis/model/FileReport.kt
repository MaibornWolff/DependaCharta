package de.maibornwolff.dependacharta.pipeline.analysis.model

import kotlinx.serialization.Serializable

@Serializable
data class FileReport(
    val nodes: List<Node>
)
