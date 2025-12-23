package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterJavascript

class JavascriptCommonJsExportsQuery(
    javascript: TreeSitterJavascript
) {
    private val moduleExportsQuery =
        TSQuery(
            javascript,
            """
        (assignment_expression
          left: (member_expression
            object: (identifier) @module
            property: (property_identifier) @exports)
          right: (identifier) @name)
            """.trimIndent()
        )

    private val exportsPropertyQuery =
        TSQuery(
            javascript,
            """
        (assignment_expression
          left: (member_expression
            object: (identifier) @exports
            property: (property_identifier) @name)
          right: (_))
            """.trimIndent()
        )

    private val moduleExportsPropertyQuery =
        TSQuery(
            javascript,
            """
        (assignment_expression
          left: (member_expression
            object: (member_expression
              object: (identifier) @module
              property: (property_identifier) @exports)
            property: (property_identifier) @name)
          right: (_))
            """.trimIndent()
        )

    fun execute(
        node: TSNode,
        body: String
    ): Set<String> {
        val moduleExports = node
            .execute(moduleExportsQuery)
            .filter { match ->
                nodeAsString(match.captures[0].node, body) == "module" &&
                    nodeAsString(match.captures[1].node, body) == "exports"
            }.map { match -> nodeAsString(match.captures[2].node, body) }

        val exportsProperties = node
            .execute(exportsPropertyQuery)
            .filter { match -> nodeAsString(match.captures[0].node, body) == "exports" }
            .map { match -> nodeAsString(match.captures[1].node, body) }

        val moduleExportsProperties = node
            .execute(moduleExportsPropertyQuery)
            .filter { match ->
                nodeAsString(match.captures[0].node, body) == "module" &&
                    nodeAsString(match.captures[1].node, body) == "exports"
            }.map { match -> nodeAsString(match.captures[2].node, body) }

        val defaultExports = if (moduleExports.isNotEmpty()) {
            moduleExports.toSet()
        } else {
            emptySet()
        }

        return defaultExports + exportsProperties + moduleExportsProperties
    }
}
