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

class JavascriptCommonJsRequireQuery(
    javascript: TreeSitterJavascript
) {
    // Matches: const foo = require('bar')
    private val simpleRequireQuery =
        TSQuery(
            javascript,
            """
        (variable_declarator
          name: (identifier) @name
          value: (call_expression
            function: (identifier) @require
            arguments: (arguments (string) @source)))
            """.trimIndent()
        )

    // Matches: const { foo, bar } = require('baz')
    private val destructuredRequireQuery =
        TSQuery(
            javascript,
            """
        (variable_declarator
          name: (object_pattern
            (shorthand_property_identifier_pattern) @name)
          value: (call_expression
            function: (identifier) @require
            arguments: (arguments (string) @source)))
            """.trimIndent()
        )

    fun execute(
        node: TSNode,
        body: String,
        currentPath: Path
    ): List<Dependency> {
        val simpleRequires = node
            .execute(simpleRequireQuery)
            .filter { match -> nodeAsString(match.captures[1].node, body) == "require" }
            .map { match ->
                val sourcePath = extractSourcePath(match.captures[2].node, body, currentPath)
                Dependency(sourcePath + DEFAULT_EXPORT_NAME)
            }

        val destructuredRequires = node
            .execute(destructuredRequireQuery)
            .filter { match ->
                val requireNode = match.captures.find { it.node.type == "identifier" && nodeAsString(it.node, body) == "require" }
                requireNode != null
            }.flatMap { match ->
                val sourcePath = extractSourcePath(match.captures.last().node, body, currentPath)
                match.captures.filter { it.node.type == "shorthand_property_identifier_pattern" }.map { capture ->
                    val importName = nodeAsString(capture.node, body)
                    Dependency(sourcePath + importName)
                }
            }

        return simpleRequires + destructuredRequires
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
