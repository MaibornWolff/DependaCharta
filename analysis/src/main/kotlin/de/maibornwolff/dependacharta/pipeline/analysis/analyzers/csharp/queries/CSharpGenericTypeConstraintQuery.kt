package de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.extractType
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import org.treesitter.TSNode

/**
 * Query to extract generic type parameter constraints from C# declarations.
 *
 * Handles constraints such as:
 * - `where T : IFooBar`
 * - `where T : IFooBar, IBarBaz`
 * - `where T : BaseClass, IFooBar`
 * - `where T : IFooBar where U : IBarBaz`
 */
class CSharpGenericTypeConstraintQuery {
    private val query = CSharpQueryFactory.getQuery(
        """
        (type_parameter_constraints_clause 
            (type_parameter_constraint 
                [
                    (identifier) @constraint
                    (qualified_name) @constraint
                    (generic_name) @constraint
                ]))
    """
    )

    /**
     * Returns the types used in generic type parameter constraints.
     *
     * Example:
     * ```
     * public class MyGeneric<T> where T : IFooBar, IBarBaz {}
     * ```
     * will return Types containing `IFooBar` and `IBarBaz`.
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return set of [de.maibornwolff.codegraph.pipeline.analysis.model.Type] objects representing the constraint types
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { extractType(it.captures[0].node, bodyContainingNode) }
        .toSet()
}