package de.maibornwolff.codegraph.pipeline.analysis.analyzers.java.queries

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.java.extractType
import de.maibornwolff.codegraph.pipeline.analysis.model.Type
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TSQueryMatch
import org.treesitter.TreeSitterJava

/**
 *  [execute]
 */
class JavaMethodAndConstructorQuery(
    val java: TreeSitterJava
) {
    private val methodQuery = TSQuery(java, "(method_declaration parameters: (formal_parameters) @parameters) @method")
    private val constructorQuery = TSQuery(java, "(constructor_declaration parameters: (formal_parameters) @parameters) @constructor")

    /**
     * Extracts the parameter and return types of the methods and constructors of a given node.
     *
     * Example:
     * ```
     * public class MyClass {
     *   public MyClass(SomeParameter parameter) { }
     *   public ReturnType myMethod(AnotherParameter parameter) { }
     * }
     * ```
     * will return Types containing `SomeParameter`, `ReturnType` and `AnotherParameter`
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [Type] objects representing the types of the
     * method/constructor parameters and the return type
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = extractConstructorParameterTypes(node, bodyContainingNode) + extractMethodTypes(node, bodyContainingNode)

    private fun extractConstructorParameterTypes(
        declaration: TSNode,
        bodyContainingNode: String
    ) = declaration.execute(constructorQuery).flatMap { extractMethodParameters(it, bodyContainingNode) }

    private fun extractMethodTypes(
        declaration: TSNode,
        bodyContainingNode: String
    ) = declaration.execute(methodQuery).flatMap {
        extractMethodParameters(it, bodyContainingNode) + extractMethodReturnType(it, bodyContainingNode)
    }

    private fun extractMethodReturnType(
        match: TSQueryMatch,
        nodeBody: String
    ): Type {
        val capturedNode = match.captures[0].node
        val typeNode = capturedNode.getChildByFieldName("type")
        return extractType(typeNode, nodeBody)
    }

    private fun extractMethodParameters(
        match: TSQueryMatch,
        bodyContainingNode: String
    ): List<Type> {
        val parametersNode = match.captures[1].node
        return parametersNode.getNamedChildren().map {
            val parameterType = it.getChildByFieldName("type")
            extractType(parameterType, bodyContainingNode)
        }
    }
}
