package de.maibornwolff.codegraph.pipeline.analysis.analyzers.java.queries

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.java.extractType
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterJava

/**
 *  [execute]
 */
class JavaThrownTypesQuery(
    val java: TreeSitterJava
) {
    private val query = TSQuery(java, "(throws) @throws")

    /**
     * Extracts the types of Throwables that might be thrown by a node.
     *
     * Example:
     * ```
     * public class MyClass {
     *    public void myMethod() throws MyException {}
     * }
     * ```
     * will return Types containing `MyException`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [de.maibornwolff.codegraph.pipeline.analysis.model.Type] objects representing the types of the
     * Throwables that might be thrown by the node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .flatMap { match ->
            match.captures[0]
                .node
                .getNamedChildren()
                .map { extractType(it, bodyContainingNode) }
        }
}
