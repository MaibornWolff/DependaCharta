package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.extractType
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import org.treesitter.TSNode

/**
 *  [execute]
 */
class CSharpMemberAccessesQuery {
    private val query = CSharpQueryFactory.getQuery("(member_access_expression expression: _ @member)")

    /**
     * Returns the types of static member accesses within the given node. Only uppercase accesses are returned.
     *
     * Example:
     * ```
     * public class MyGreatClass {
     *     public MyType myVar = MyType.MyStaticMember; // Will be returned, because MyType is uppercase
     *     public int value = myVar.someFunction(); // Will not be returned because myVar is not uppercase
     * }
     * ```
     * will return Types containing `MyType`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [de.maibornwolff.dependacharta.pipeline.analysis.model.Type] objects representing the types of the
     * static member accesses within this node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { extractType(it.captures[0].node, bodyContainingNode) }
        .filter { it.isUppercase() }
        .toSet()
}
