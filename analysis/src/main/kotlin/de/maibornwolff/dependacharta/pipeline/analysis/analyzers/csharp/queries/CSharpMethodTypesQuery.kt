package de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.extractType
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import org.treesitter.TSNode

/**
 *  [execute]
 */
class CSharpMethodTypesQuery {
    private val query = CSharpQueryFactory.getQuery("(method_declaration parameters: (parameter_list) @parameters) @method")

    /**
     * Returns the types of the parameters of all constructors contained within the given node.
     *
     * Example:
     * ```
     * public class MyGreatClass {
     *     public ReturnType myGreatFunction(ParameterType myGreatParameter) {}
     * }
     * ```
     * will return Types containing `ReturnType` and `ParameterType`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [de.maibornwolff.codegraph.pipeline.analysis.model.Type] objects representing the types of the
     * method parameters and the return type
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node.execute(query).flatMap {
        val methodNode = it.captures[0].node
        val parametersNode = it.captures[1].node
        val returnType = extractType(methodNode.getChildByFieldName("returns"), bodyContainingNode)
        val parameterTypes = parametersNode
            .getNamedChildren()
            .map { typeNode -> extractType(typeNode.getChildByFieldName("type"), bodyContainingNode) }
        parameterTypes + returnType
    }
}
