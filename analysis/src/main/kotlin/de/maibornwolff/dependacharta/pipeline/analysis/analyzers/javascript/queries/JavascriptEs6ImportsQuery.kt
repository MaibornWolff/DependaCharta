package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.ImportPathResolver
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.DEFAULT_EXPORT_NAME
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.model.DependenciesAndAliases
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
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

    private val importPathResolver = ImportPathResolver()

    fun execute(
        node: TSNode,
        body: String,
        currentPath: Path,
        fileInfo: FileInfo? = null
    ): DependenciesAndAliases {
        val namedImportResults = node.execute(namedImportQuery).flatMap { match ->
            val sourcePath = extractSourcePath(match.captures.last().node, body, currentPath, fileInfo)
            match.captures.dropLast(1).map { capture ->
                val importName = nodeAsString(capture.node, body)
                ImportResult(
                    dependency = Dependency(sourcePath + importName),
                    localName = importName
                )
            }
        }

        val defaultImportResults = node.execute(defaultImportQuery).map { match ->
            val nameNode = match.captures.first().node
            val sourceNode = match.captures.last().node
            val localName = nodeAsString(nameNode, body)
            val sourceString = nodeAsString(sourceNode, body).trim('"', '\'')
            val sourcePath = extractSourcePath(sourceNode, body, currentPath, fileInfo)
            // Vue components don't use .default suffix - the component itself is the default export
            val dependency = if (sourceString.endsWith(".vue")) {
                Dependency(sourcePath)
            } else {
                Dependency(sourcePath + DEFAULT_EXPORT_NAME)
            }
            ImportResult(dependency = dependency, localName = localName)
        }

        val namespaceImportResults = node.execute(namespaceImportQuery).map { match ->
            val nameNode = match.captures.first().node
            val localName = nodeAsString(nameNode, body)
            val sourcePath = extractSourcePath(match.captures.last().node, body, currentPath, fileInfo)
            ImportResult(
                dependency = Dependency(sourcePath, isWildcard = true),
                localName = localName
            )
        }

        val allResults = namedImportResults + defaultImportResults + namespaceImportResults

        return DependenciesAndAliases(
            dependencies = allResults.map { it.dependency }.toSet(),
            importByAlias = emptyMap(),
            usedTypes = allResults.map { Type.simple(it.localName) }.toSet()
        )
    }

    private data class ImportResult(
        val dependency: Dependency,
        val localName: String
    )

    private fun extractSourcePath(
        sourceNode: TSNode,
        body: String,
        currentPath: Path,
        fileInfo: FileInfo?
    ): Path {
        val sourceString = nodeAsString(sourceNode, body).trim('"', '\'')
        return importPathResolver.resolve(sourceString, currentPath, fileInfo)
    }
}
