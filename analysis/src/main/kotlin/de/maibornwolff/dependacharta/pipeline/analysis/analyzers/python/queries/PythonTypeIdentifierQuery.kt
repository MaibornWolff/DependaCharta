package de.maibornwolff.codegraph.pipeline.analysis.analyzers.python.queries

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterPython

class PythonTypeIdentifierQuery(
    val python: TreeSitterPython
) {
    private val query = TSQuery(python, "(identifier) @identifier")

    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node.execute(query).map { it.captures[0].node }.map {
        nodeAsString(it, bodyContainingNode)
    }
}
