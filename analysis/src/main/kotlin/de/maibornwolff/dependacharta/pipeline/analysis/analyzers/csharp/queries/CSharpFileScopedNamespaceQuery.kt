package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import org.treesitter.TSNode

/**
 *  [execute]
 */
class CSharpFileScopedNamespaceQuery {
    private val query = CSharpQueryFactory.getQuery(
        "(file_scoped_namespace_declaration [(qualified_name) (identifier)] @name)".trimIndent()
    )

    /**
     * Executes the query on the given node and returns the file scoped namespace if it exists.
     *
     * Example:
     * ```
     * namespace My.Great.Namespace;
     * ```
     * will return `listOf("My", "Great", "Namespace")`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return the file scoped namespace as a list of strings or null if it does not exist
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map {
            nodeAsString(it.captures[0].node, bodyContainingNode).split(".")
        }.firstOrNull()
}
