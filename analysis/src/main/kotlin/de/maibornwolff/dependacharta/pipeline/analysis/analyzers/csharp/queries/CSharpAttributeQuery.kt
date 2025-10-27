package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.extractType
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import org.treesitter.TSNode

/**
 *  [execute]
 */
class CSharpAttributeQuery {
    private val query = CSharpQueryFactory.getQuery("(attribute name: _ @name)")

    /**
     * Returns the types of static member accesses within the given node. Only uppercase accesses are returned.
     *
     * Example:
     * ```
     * [MyAttribute]
     * public class MyGreatClass {}
     * ```
     * will return Types containing `MyAttribute`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [de.maibornwolff.dependacharta.pipeline.analysis.model.Type] objects representing the types of the
     * used attributes within this node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { extractType(it.captures[0].node, bodyContainingNode) }
}
