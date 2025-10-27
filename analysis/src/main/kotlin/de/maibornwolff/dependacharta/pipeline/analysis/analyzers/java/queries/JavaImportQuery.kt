package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.java.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterJava

/**
 *  [execute]
 */
class JavaImportQuery(
    val java: TreeSitterJava
) {
    private val importQuery = TSQuery(java, "(import_declaration) @import")
    private val identifierQuery = TSQuery(java, "(identifier) @identifiers")

    /**
     * Returns the dependencies imported in the given node.
     *
     * Example:
     * ```
     * import my.great.package.MyGreatFile;
     * import my.wildcard.import.*;
     * ```
     * will result in
     * ```
     * Depdendency(Path(listOf("my", "great", "package", "MyGreatFile")), isWildcard = false) and
     * Depdendency(Path(listOf("my", "wildcard", "import")), isWildcard = true)
     * ```
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [Dependency] objects representing the imports of a file
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node.execute(importQuery).map {
        val capturedNode = it.captures[0].node
        val isWildcard = capturedNode.namedChildCount == 2 && capturedNode.getNamedChild(1).type == "asterisk"
        val packageNames = extractPackageNames(capturedNode, bodyContainingNode)
        Dependency(Path(packageNames), isWildcard)
    }

    private fun extractPackageNames(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(identifierQuery)
        .flatMap { it.captures.map { capture -> nodeAsString(capture.node, bodyContainingNode) } }
}
