package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.java.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.java.extractType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterJava

/**
 *  [execute]
 */
class JavaInheritanceQuery(
    val java: TreeSitterJava
) {
    private val implementsAndExtendsQuery = TSQuery(java, "[(super_interfaces) (extends_interfaces)] @implementsStatement")
    private val superclassQuery = TSQuery(java, "(superclass) @superclass")

    /**
     * Extracts the parameter and return types of the methods and constructors of a given node.
     *
     * Example:
     * ```
     * public interface MyInterface extends SomeInterface {} // returns SomeInterface
     * public class MyClass implements MyInterface {} // returns MyInterface
     * public class MySubClass extends MyClass {} // returns MyClass
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
        val inheritanceTypes = node.execute(implementsAndExtendsQuery).flatMap {
            it.captures.flatMap { capture ->
                val typeListNode = capture.node.getNamedChild(0)
                val namedChildren = typeListNode.getNamedChildren()
                namedChildren.map { child -> extractType(child, bodyContainingNode) }
            }
        }
        val superClasses = node.execute(superclassQuery).flatMap {
            it.captures.map { capture ->
                extractType(capture.node.getNamedChild(0), bodyContainingNode)
            }
        }
        return inheritanceTypes + superClasses
    }
}
