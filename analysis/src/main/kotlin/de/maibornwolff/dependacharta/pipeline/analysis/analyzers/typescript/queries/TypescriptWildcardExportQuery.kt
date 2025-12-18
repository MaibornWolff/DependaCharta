package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterTypescript

/**
 * Query to extract wildcard re-export statements from TypeScript/TSX files.
 * Example: export * from './constants'
 */
class TypescriptWildcardExportQuery(
    typescript: TreeSitterTypescript
) {
    private val query = TSQuery(
        typescript,
        """
        (export_statement
          "*" @star
          source: (string) @source)
        """
    )

    /**
     * Returns list of source paths for wildcard exports.
     * Example: ["./constants", "../utils"]
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ): List<String> {
        return node
            .execute(query)
            .map { match ->
                // captures[0] is "*", captures[1] is the source string
                val sourceNode = match.captures[1].node
                nodeAsString(sourceNode, bodyContainingNode)
            }
    }
}
