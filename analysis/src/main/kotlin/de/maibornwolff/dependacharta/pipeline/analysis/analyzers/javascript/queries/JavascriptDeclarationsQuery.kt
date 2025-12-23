package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.JavascriptDeclaration
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterJavascript

class JavascriptDeclarationsQuery(
    javascript: TreeSitterJavascript
) {
    private val classQuery = TSQuery(javascript, "(class_declaration name: (identifier) @name)")
    private val functionQuery = TSQuery(javascript, "(function_declaration name: (identifier) @name)")
    private val variableQuery =
        TSQuery(
            javascript,
            """
        (lexical_declaration
          (variable_declarator
            name: (identifier) @name))
            """.trimIndent()
        )
    private val varDeclarationQuery =
        TSQuery(
            javascript,
            """
        (variable_declaration
          (variable_declarator
            name: (identifier) @name))
            """.trimIndent()
        )

    private val defaultExportClassQuery =
        TSQuery(
            javascript,
            """
        (export_statement (class_declaration) @decl)
            """.trimIndent()
        )

    private val defaultExportFunctionQuery =
        TSQuery(
            javascript,
            """
        (export_statement (function_declaration) @decl)
            """.trimIndent()
        )

    fun execute(
        node: TSNode,
        body: String
    ): List<JavascriptDeclaration> {
        val classes = node
            .execute(classQuery)
            .map { JavascriptDeclaration(nodeAsString(it.captures[0].node, body), NodeType.CLASS) }

        val functions = node
            .execute(functionQuery)
            .map { JavascriptDeclaration(nodeAsString(it.captures[0].node, body), NodeType.FUNCTION) }

        val variables = node
            .execute(variableQuery)
            .map { JavascriptDeclaration(nodeAsString(it.captures[0].node, body), NodeType.VARIABLE) }

        val varDeclarations = node
            .execute(varDeclarationQuery)
            .map { JavascriptDeclaration(nodeAsString(it.captures[0].node, body), NodeType.VARIABLE) }

        // Add anonymous default exports
        val defaultExports = extractDefaultExports(node, body)

        return classes + functions + variables + varDeclarations + defaultExports
    }

    private fun extractDefaultExports(
        node: TSNode,
        body: String
    ): List<JavascriptDeclaration> {
        val defaultClasses = node
            .execute(defaultExportClassQuery)
            .filter { match ->
                val declNode = match.captures[0].node
                val exportStmt = declNode.parent
                // Check if this export statement has "default" keyword
                exportStmt != null &&
                    body.substring(exportStmt.startByte, exportStmt.endByte).contains("default")
            }.map { JavascriptDeclaration("default", NodeType.CLASS) }

        val defaultFunctions = node
            .execute(defaultExportFunctionQuery)
            .filter { match ->
                val declNode = match.captures[0].node
                val exportStmt = declNode.parent
                exportStmt != null &&
                    body.substring(exportStmt.startByte, exportStmt.endByte).contains("default")
            }.map { JavascriptDeclaration("default", NodeType.FUNCTION) }

        return defaultClasses + defaultFunctions
    }
}
