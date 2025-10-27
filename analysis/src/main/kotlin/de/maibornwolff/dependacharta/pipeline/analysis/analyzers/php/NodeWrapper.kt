package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.php

import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import org.treesitter.TSNode

data class NodeWrapper(
    val treesitterNode: TSNode,
    val nodeType: NodeType,
    val getName: () -> String,
)