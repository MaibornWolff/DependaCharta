package de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import org.treesitter.TSNode

/**
 *  [execute]
 */
class CSharpNamespaceQuery {
    private val query = CSharpQueryFactory.getQuery("(namespace_declaration) @namespace")

    /**
     * Returns the C# namespaces contained within the given node.
     *
     * Example:
     * ```
     * namespace A.Great.Namespace {
     *    enum MyGreatEnum {}
     * }
     * ```
     * will return the TreeSitter nodes representing `A.Great.Namespace`.
     *
     * @param node the node to execute the query on
     * @return list of [TSNode]s representing the namespaces
     */
    fun execute(node: TSNode) = node.execute(query).map { it.captures[0].node }
}
