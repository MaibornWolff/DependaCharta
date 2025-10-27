package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.extractType
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import org.treesitter.TSNode

/**
 *  [execute]
 */
class CSharpVariableDeclarationQuery {
    private val query = CSharpQueryFactory.getQuery("(variable_declaration) @variable")

    /**
     * Returns the types of declared variables.
     *
     * Example:
     * ```
     * public class MyGreatClass {
     *     public MyType myVar = 42;
     * }
     * ```
     * will return Types containing `MyType`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [de.maibornwolff.dependacharta.pipeline.analysis.model.Type] objects representing the types of the
     * declared variables within the node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map {
            val capturedNode = it.captures[0].node
            val typeNode = capturedNode.getChildByFieldName("type")
            extractType(typeNode, bodyContainingNode)
        }.filterNot { it.name == "var" || it.name == "void" }
}
