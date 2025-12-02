package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterKotlin

/**
 *  [execute]
 */
class KotlinPackageQuery(
    val kotlin: TreeSitterKotlin
) {
    private val packageQuery = TSQuery(kotlin, "(package_header) @package")
    private val identifierQuery = TSQuery(kotlin, "(simple_identifier) @identifiers")

    /**
     * Returns the package declared in the given node.
     *
     * Example:
     * ```
     * package my.great.package
     * ```
     * will return listOf("my", "great", "package")
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return package as a list of strings if there is a package declaration, otherwise an empty list
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) =
        node
            .execute(packageQuery)
            .map {
                val capturedNode = it.captures[0].node
                extractPackageNames(capturedNode, bodyContainingNode)
            }.firstOrNull() ?: emptyList()

    private fun extractPackageNames(
        node: TSNode,
        bodyContainingNode: String
    ) =
        node
            .execute(identifierQuery)
            .flatMap { it.captures.map { capture -> nodeAsString(capture.node, bodyContainingNode) } }
}
