package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.java.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterJava

/**
 *  [execute]
 */
class JavaDeclarationsQuery(
    val java: TreeSitterJava
) {
    private val query =
        TSQuery(
            java,
            """
        [
         (class_declaration)
         (record_declaration)
         (interface_declaration)
         (enum_declaration)
         (annotation_type_declaration)
        ] @declaration
            """.trimIndent()
        )

    /**
     * Returns the Java declarations contained within the given node.
     *
     * Example:
     * ```
     * class MyGreatClass {}
     * enum MyGreatEnum {}
     * ```
     * will return the TreeSitter nodes representing `MyGreatClass` and `MyGreatEnum`.
     *
     * @param node the node to execute the query on
     * @return list of [TSNode]s representing the declarations
     */
    fun execute(node: TSNode): List<TSNode> =
        node
            .execute(query)
            .map { it.captures[0].node }
}
