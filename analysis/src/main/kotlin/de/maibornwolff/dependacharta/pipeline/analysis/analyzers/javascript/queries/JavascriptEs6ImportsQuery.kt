package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.bundler.BundlerAliasResolver
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.bundler.BundlerConfigResolver
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.federation.FederationAliasResolver
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.federation.FederationConfigResolver
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.DirectImport
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.toImport
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.resolveRelativePath
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.stripSourceFileExtension
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.DEFAULT_EXPORT_NAME
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.model.DependenciesAndAliases
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.tsconfig.PathAliasResolver
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.tsconfig.TsConfigResolver
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

    private val tsConfigResolver = TsConfigResolver()
    private val bundlerConfigResolver = BundlerConfigResolver()
    private val federationConfigResolver = FederationConfigResolver()

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
        val trimmedSourceString = sourceString.stripSourceFileExtension()
        val import = trimmedSourceString.toImport()

        if (import is DirectImport && fileInfo?.analysisRoot != null) {
            val sourceFile = fileInfo.analysisRoot.resolve(fileInfo.physicalPath)

            // Try jsconfig.json/tsconfig.json path resolution first
            val tsConfigResult = tsConfigResolver.findTsConfig(sourceFile)
            if (tsConfigResult != null) {
                val resolved = PathAliasResolver.resolve(
                    import,
                    tsConfigResult.data,
                    tsConfigResult.file.parentFile,
                    fileInfo.analysisRoot
                )
                if (resolved != null) {
                    return resolved
                }
            }

            // Fall back to bundler config aliases (webpack, vite, vue.config)
            val bundlerResult = bundlerConfigResolver.findBundlerConfig(sourceFile)
            if (bundlerResult != null) {
                val resolved = BundlerAliasResolver.resolve(
                    import,
                    bundlerResult.data,
                    fileInfo.analysisRoot
                )
                if (resolved != null) {
                    return resolved
                }
            }

            // Fall back to Module Federation remotes
            val federationResult = federationConfigResolver.findConsumerConfig(sourceFile)
            if (federationResult != null) {
                val resolved = FederationAliasResolver.resolve(
                    import,
                    federationResult,
                    federationConfigResolver,
                    fileInfo.analysisRoot
                )
                if (resolved != null) {
                    return resolved
                }
            }
        }

        // Fall back to relative path resolution
        return resolveRelativePath(import, currentPath)
    }
}
