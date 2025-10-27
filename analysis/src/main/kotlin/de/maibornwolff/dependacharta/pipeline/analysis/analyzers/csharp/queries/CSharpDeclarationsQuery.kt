package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import org.treesitter.TSNode

/**
 *  [execute]
 */
class CSharpDeclarationsQuery {
    private val query = CSharpQueryFactory.getQuery("(type_declaration) @declaration")

    /**
     * Returns the C# declarations contained within the given node.
     *
     * Example:
     * ```
     * class MyGreatClass {}
     * enum MyGreatEnum {}
     * ```
     * will return the TreeSitter nodes representing `MyGreatClass` and `MyGreatEnum`.
     *
     * @param node the node to execute the query on
     * @return list of [TSNode]s representing the declarations
     */
    fun execute(node: TSNode): List<TSNode> =
        node
            .execute(query)
            .map { it.captures[0].node }
}
