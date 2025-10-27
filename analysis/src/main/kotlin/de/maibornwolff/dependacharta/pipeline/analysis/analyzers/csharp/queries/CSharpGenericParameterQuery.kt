package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.extractType
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import org.treesitter.TSNode

class CSharpGenericParameterQuery {
    private val query = CSharpQueryFactory.getQuery("(type_argument_list (identifier) @type_arguments)")

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
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { extractType(it.captures[0].node, bodyContainingNode) }
        .toSet()
}