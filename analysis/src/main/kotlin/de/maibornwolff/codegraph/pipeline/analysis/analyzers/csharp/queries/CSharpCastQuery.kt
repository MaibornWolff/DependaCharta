package de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.extractType
import org.treesitter.TSNode

class CSharpCastQuery {
    private val query = CSharpQueryFactory.getQuery(
        """
            (cast_expression type: _ @type)
            (as_expression right: _ @type)
        """
    )

    /**
     * Returns the type of the casted object within the given node.
     *
     * Example:
     * ```
     * var Foo = Bar as FooBar;
     * var Bar = (FooBar) Bar;
     * ```
     * will return Types containing `FooBar`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [de.maibornwolff.codegraph.pipeline.analysis.model.Type] objects representing the types of the
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
