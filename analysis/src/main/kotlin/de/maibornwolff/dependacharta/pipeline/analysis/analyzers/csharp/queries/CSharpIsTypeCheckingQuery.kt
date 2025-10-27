package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.extractType
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import org.treesitter.TSNode

class CSharpIsTypeCheckingQuery {
    private val query = CSharpQueryFactory.getQuery(
        """
(is_pattern_expression pattern: (declaration_pattern type: (identifier) @type))
(is_pattern_expression pattern: (constant_pattern (identifier) @type))
        """.trimIndent()
    )

    /**
     * Returns the type of the is expression type arguments within the given node.
     *
     * Example:
     * ```
     * if (foo is Type1 bar) {]
     * if (foo is Type1) {}
     * ```
     * will return Types 'Type1'
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [de.maibornwolff.dependacharta.pipeline.analysis.model.Type] objects representing the types of the
     * created objects within this node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { extractType(it.captures[0].node, bodyContainingNode) }
        .toSet()
}