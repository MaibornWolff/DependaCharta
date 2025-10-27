package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.extractType
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import org.treesitter.TSNode

/**
 *  [execute]
 */
class CSharpConstructorParameterQuery {
    private val query = CSharpQueryFactory.getQuery(
        "(constructor_declaration parameters: (parameter_list) @parameters)" +
            "(class_declaration  (parameter_list) @parameters)"
    )

    /**
     * Returns the types of the parameters of all constructors including primary constructors
     * contained within the given node.
     *
     * Example:
     * ```
     * public class MyGreatClass (string myPrimaryParameter) {
     *     public MyGreatClass(int myGreatParameter) {}
     * }
     * ```
     * will return a Type containing `string` and `int` respectively
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [de.maibornwolff.dependacharta.pipeline.analysis.model.Type] objects representing the types of the constructor parameters
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node.execute(query).flatMap {
        it.captures[0]
            .node
            .getNamedChildren()
            .map { typeNode -> extractType(typeNode.getChildByFieldName("type"), bodyContainingNode) }
    }
}
