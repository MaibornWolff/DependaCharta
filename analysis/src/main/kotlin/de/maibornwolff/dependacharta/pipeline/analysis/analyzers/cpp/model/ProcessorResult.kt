package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.model

import de.maibornwolff.dependacharta.pipeline.analysis.model.Node

data class ProcessorResult(
    val nodes: List<Node>,
    val context: CppContext
)
