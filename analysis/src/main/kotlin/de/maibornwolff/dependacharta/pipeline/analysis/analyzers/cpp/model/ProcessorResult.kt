package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model

import de.maibornwolff.codegraph.pipeline.analysis.model.Node

data class ProcessorResult(
    val nodes: List<Node>,
    val context: CppContext
)
