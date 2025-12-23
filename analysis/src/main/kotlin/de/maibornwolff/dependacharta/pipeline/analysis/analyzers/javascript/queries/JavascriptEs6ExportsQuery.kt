package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.toImport
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.resolveRelativePath
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.DEFAULT_EXPORT_NAME
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.JavascriptReexport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterJavascript

private const val DEFAULT_KEYWORD = "default"

class JavascriptEs6ExportsQuery(
    private val javascript: TreeSitterJavascript
) {
    private val namedExportQuery =
        TSQuery(
            javascript,
            """
        (export_statement
          (class_declaration name: (identifier) @name))
            """.trimIndent()
        )

    private val functionExportQuery =
        TSQuery(
            javascript,
            """
        (export_statement
          (function_declaration name: (identifier) @name))
            """.trimIndent()
        )

    private val variableExportQuery =
        TSQuery(
            javascript,
            """
        (export_statement
          (lexical_declaration
            (variable_declarator name: (identifier) @name)))
            """.trimIndent()
        )

    private val defaultExportQuery = TSQuery(javascript, "(export_statement (identifier) @name)")

    private val exportListQuery =
        TSQuery(
            javascript,
            """
        (export_statement
          (export_clause
            (export_specifier name: (identifier) @name)))
            """.trimIndent()
        )

    private val reexportNamedQuery =
        TSQuery(
            javascript,
            """
        (export_statement
          (export_clause
            (export_specifier name: (identifier) @name))
          source: (string) @source)
            """.trimIndent()
        )

    private val reexportWildcardQuery =
        TSQuery(
            javascript,
            """
        (export_statement
          "*"
          source: (string) @source)
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

    fun executeExports(
        node: TSNode,
        body: String
    ): ExportsInfo {
        val namedExports = node
            .execute(namedExportQuery)
            .map { nodeAsString(it.captures[0].node, body) }

        val functionExports = node
            .execute(functionExportQuery)
            .map { nodeAsString(it.captures[0].node, body) }

        val variableExports = node
            .execute(variableExportQuery)
            .map { nodeAsString(it.captures[0].node, body) }

        val exportListExports = node
            .execute(exportListQuery)
            .filter { match ->
                val exportStatement = match.captures[0]
                    .node.parent
                    ?.parent
                val childCount = exportStatement?.childCount ?: 0
                (0 until childCount).none { i ->
                    exportStatement?.getChild(i)?.type == "string"
                }
            }.map { nodeAsString(it.captures[0].node, body) }

        val defaultExportMatches = node
            .execute(defaultExportQuery)
            .filter { match ->
                val exportStatement = match.captures[0].node.parent
                val childCount = exportStatement?.childCount ?: 0
                (0 until childCount).none { i ->
                    exportStatement?.getChild(i)?.type == "string"
                }
            }

        val defaultExportedIdentifier = defaultExportMatches
            .map { nodeAsString(it.captures[0].node, body) }
            .firstOrNull()

        val defaultExports = if (defaultExportMatches.isNotEmpty()) {
            listOf(DEFAULT_EXPORT_NAME)
        } else {
            emptyList()
        }

        val defaultClasses = node
            .execute(defaultExportClassQuery)
            .filter { match ->
                val declNode = match.captures[0].node
                val exportStmt = declNode.parent
                exportStmt != null &&
                    body.substring(exportStmt.startByte, exportStmt.endByte).contains(DEFAULT_KEYWORD)
            }.map { DEFAULT_EXPORT_NAME }

        val defaultFunctions = node
            .execute(defaultExportFunctionQuery)
            .filter { match ->
                val declNode = match.captures[0].node
                val exportStmt = declNode.parent
                exportStmt != null &&
                    body.substring(exportStmt.startByte, exportStmt.endByte).contains(DEFAULT_KEYWORD)
            }.map { DEFAULT_EXPORT_NAME }

        val inlineDefaultExports = defaultClasses + defaultFunctions

        val allExports = (
            namedExports + functionExports + variableExports + exportListExports + defaultExports + inlineDefaultExports
        ).toSet()

        return ExportsInfo(allExports, defaultExportedIdentifier)
    }

    fun executeReexports(
        node: TSNode,
        body: String,
        currentPath: Path
    ): List<JavascriptReexport> {
        val namedReexports = node.execute(reexportNamedQuery).flatMap { match ->
            val sourcePath = extractSourcePath(match.captures.last().node, body, currentPath)
            match.captures.dropLast(1).map { capture ->
                val exportName = nodeAsString(capture.node, body)
                JavascriptReexport(exportName, Dependency(sourcePath + exportName))
            }
        }

        val wildcardReexports = node.execute(reexportWildcardQuery).map { match ->
            val sourcePath = extractSourcePath(match.captures[0].node, body, currentPath)
            JavascriptReexport("*", Dependency(sourcePath, isWildcard = true))
        }

        return namedReexports + wildcardReexports
    }

    private fun extractSourcePath(
        sourceNode: TSNode,
        body: String,
        currentPath: Path
    ): Path {
        val sourceString = nodeAsString(sourceNode, body).trim('"', '\'')
        val import = sourceString.toImport()
        return resolveRelativePath(import, currentPath)
    }
}

data class ExportsInfo(
    val exports: Set<String>,
    val defaultExportedIdentifier: String?
)
