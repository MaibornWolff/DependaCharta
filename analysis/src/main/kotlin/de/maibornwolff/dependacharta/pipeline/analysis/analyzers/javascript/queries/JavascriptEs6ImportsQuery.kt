package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.toImport
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.resolveRelativePath
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.DEFAULT_EXPORT_NAME
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterJavascript

class JavascriptEs6ImportsQuery(
    javascript: TreeSitterJavascript
) {
    private val namedImportQuery =
        TSQuery(
            javascript,
            """
        (import_statement
          (import_clause
            (named_imports
              (import_specifier name: (identifier) @name)))
          source: (string) @source)
            """.trimIndent()
        )

    private val defaultImportQuery =
        TSQuery(
            javascript,
            """
        (import_statement
          (import_clause (identifier) @name)
          source: (string) @source)
            """.trimIndent()
        )

    private val namespaceImportQuery =
        TSQuery(
            javascript,
            """
        (import_statement
          (import_clause
            (namespace_import (identifier) @name))
          source: (string) @source)
            """.trimIndent()
        )

    fun execute(
        node: TSNode,
        body: String,
        currentPath: Path
    ): List<Dependency> {
        val namedImports = node.execute(namedImportQuery).flatMap { match ->
            val sourcePath = extractSourcePath(match.captures.last().node, body, currentPath)
            match.captures.dropLast(1).map { capture ->
                val importName = nodeAsString(capture.node, body)
                Dependency(sourcePath + importName)
            }
        }

        val defaultImports = node.execute(defaultImportQuery).map { match ->
            val sourcePath = extractSourcePath(match.captures.last().node, body, currentPath)
            Dependency(sourcePath + DEFAULT_EXPORT_NAME)
        }

        val namespaceImports = node.execute(namespaceImportQuery).map { match ->
            val sourcePath = extractSourcePath(match.captures.last().node, body, currentPath)
            Dependency(sourcePath, isWildcard = true)
        }

        return namedImports + defaultImports + namespaceImports
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
