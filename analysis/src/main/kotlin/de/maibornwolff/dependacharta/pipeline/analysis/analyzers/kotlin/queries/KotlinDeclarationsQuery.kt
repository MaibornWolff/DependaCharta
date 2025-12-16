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
         (function_declaration)
        ] @declaration
            """.trimIndent()
        )

    /**
     * Returns the Kotlin declarations contained within the given node.
     * This includes classes, objects, and top-level functions.
     *
     * Example:
     * ```
     * class MyGreatClass {}
     * object MySingleton {}
     * fun myTopLevelFunction() {}
     * ```
     * will return the TreeSitter nodes representing `MyGreatClass`, `MySingleton`,
     * and `myTopLevelFunction`.
     *
     * @param node the node to execute the query on
     * @return list of [TSNode]s representing the declarations
     */
    fun execute(node: TSNode): List<TSNode> =
        node
            .execute(query)
            .map { it.captures[0].node }
}
