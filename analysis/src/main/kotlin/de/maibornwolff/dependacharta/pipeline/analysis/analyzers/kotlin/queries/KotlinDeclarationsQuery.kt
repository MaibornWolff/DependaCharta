package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterKotlin

/**
 *  [execute]
 */
class KotlinDeclarationsQuery(
    val kotlin: TreeSitterKotlin
) {
    private val query =
        TSQuery(
            kotlin,
            """
        [
         (class_declaration)
         (object_declaration)
        ] @declaration
            """.trimIndent()
        )

    /**
     * Returns the Kotlin declarations contained within the given node.
     *
     * Example:
     * ```
     * class MyGreatClass {}
     * object MySingleton {}
     * ```
     * will return the TreeSitter nodes representing `MyGreatClass` and `MySingleton`.
     *
     * @param node the node to execute the query on
     * @return list of [TSNode]s representing the declarations
     */
    fun execute(node: TSNode): List<TSNode> =
        node
            .execute(query)
            .map { it.captures[0].node }
}
