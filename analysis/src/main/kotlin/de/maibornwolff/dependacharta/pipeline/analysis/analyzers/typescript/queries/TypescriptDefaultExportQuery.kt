package de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.queries

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterTypescript

/**
 *  [execute]
 */
class TypescriptDefaultExportQuery(
    val typescript: TreeSitterTypescript
) {
    private val query = TSQuery(typescript, "(export_statement value: (_) @default_export)")

    /**
     * Returns the name of the default export of a typescript node.
     *
     * Example:
     * ```
     * class myGreatFunction() {}
     *
     * export default myGreatFunction;
     * ```
     * will return the String "myGreatFunction".
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return the name of the default export or null if there is no default export
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ): String? =
        node
            .execute(query)
            .map { it.captures[0].node }
            .map { nodeAsString(it, bodyContainingNode) }
            .firstOrNull()
}
