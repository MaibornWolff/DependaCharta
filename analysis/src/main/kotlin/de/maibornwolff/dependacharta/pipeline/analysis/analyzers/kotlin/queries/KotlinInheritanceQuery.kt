package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin.extractType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterKotlin

/**
 *  [execute]
 */
class KotlinInheritanceQuery(
    val kotlin: TreeSitterKotlin
) {
    private val delegationSpecifierQuery = TSQuery(kotlin, "(delegation_specifier) @delegation")

    /**
     * Extracts the inherited types (superclasses and interfaces) from a given node.
     *
     * Example:
     * ```
     * interface MyInterface : SomeInterface {} // returns SomeInterface
     * class MyClass : MyInterface {} // returns MyInterface
     * class MySubClass : MyClass() {} // returns MyClass
     * ```
     * will return Types containing `SomeInterface`, `MyInterface` and `MyClass`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [Type] objects representing the types of the
     * implemented/extended interfaces/classes
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ): List<Type> {
        return node.execute(delegationSpecifierQuery).mapNotNull {
            val delegationNode = it.captures[0].node
            val children = delegationNode.getNamedChildren()
            if (children.isEmpty()) return@mapNotNull null

            val firstChild = children[0]
            val typeNode = when (firstChild.type) {
                "constructor_invocation" -> firstChild.getNamedChildren().firstOrNull()
                "user_type", "nullable_type" -> firstChild
                else -> null
            }
            if (typeNode == null) return@mapNotNull null
            extractType(typeNode, bodyContainingNode)
        }
    }
}
