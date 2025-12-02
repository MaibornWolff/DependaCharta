package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterKotlin

/**
 *  [execute]
 */
class KotlinImportQuery(
    val kotlin: TreeSitterKotlin
) {
    private val importQuery = TSQuery(kotlin, "(import_header) @import")
    private val identifierQuery = TSQuery(kotlin, "(simple_identifier) @identifiers")

    /**
     * Returns the dependencies imported in the given node.
     *
     * Example:
     * ```
     * import my.great.pkg.MyGreatFile
     * import my.wildcard.import.*
     * ```
     * will result in
     * ```
     * Dependency(Path(listOf("my", "great", "pkg", "MyGreatFile")), isWildcard = false) and
     * Dependency(Path(listOf("my", "wildcard", "import")), isWildcard = true)
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
        val importBody = nodeAsString(capturedNode, bodyContainingNode)
        val isWildcard = importBody.contains("*")
        val packageNames = extractPackageNames(capturedNode, bodyContainingNode)
        Dependency(Path(packageNames), isWildcard)
    }

    private fun extractPackageNames(
        node: TSNode,
        bodyContainingNode: String
    ): List<String> =
        node
            .execute(identifierQuery)
            .flatMap { it.captures.map { capture -> nodeAsString(capture.node, bodyContainingNode) } }
}
