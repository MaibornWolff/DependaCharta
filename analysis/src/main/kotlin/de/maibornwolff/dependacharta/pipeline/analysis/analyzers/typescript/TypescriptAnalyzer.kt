package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.DirectImport
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.toImport
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.resolveRelativePath
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.stripSourceFileExtension
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.model.Declaration
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.model.DependenciesAndAliases
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.queries.*
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.tsconfig.PathAliasResolver
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.tsconfig.TsConfigResolver
import de.maibornwolff.dependacharta.pipeline.analysis.model.*
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterTsx
import org.treesitter.TreeSitterTypescript

const val DEFAULT_EXPORT_NODE_NAME = "DEFAULT_EXPORT"

class TypescriptAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    private val isTsxFile = fileInfo.physicalPath.endsWith(".tsx")

    // Use TSX grammar for .tsx files (supports JSX), TypeScript grammar for .ts files
    private val grammar = if (isTsxFile) TreeSitterTsx() else TreeSitterTypescript()

    private val declarationsQuery = TypescriptDeclarationsQuery(grammar)
    private val defaultExportQuery = TypescriptDefaultExportQuery(grammar)
    private val importStatementQuery = TypescriptImportStatementQuery(grammar)
    private val indexTsExportQuery = TypescriptIndexTsExportQuery(grammar)
    private val wildcardExportQuery = TypescriptWildcardExportQuery(grammar)
    private val typeIdentifierQuery = TypescriptTypeIdentifierQuery(grammar)
    private val constructorCallQuery = TypescriptConstructorCallQuery(grammar)
    private val memberExpressionQuery = TypescriptMemberExpressionQuery(grammar)
    private val extendsClauseQuery = TypescriptExtendsClauseQuery(grammar)
    private val methodCallIdentifierQuery = TypescriptMethodCallIdentifierQuery(grammar)
    private val identifierQuery = TypescriptIdentifierQuery(grammar)

    override fun analyze(): FileReport {
        val rootNode = parseCode(fileInfo.content)

        val pathFromInfo = fileInfo.physicalPathAsPath()
        val fileName = pathFromInfo.getName()
        val fileSuffix = if (isTsxFile) "tsx" else "ts"
        val path = pathFromInfo.withoutFileSuffix(fileSuffix)

        val fileLevelDependencies = importStatementQuery.execute(rootNode, fileInfo.content, path, fileInfo)
        val reexports = extractReexports(rootNode, fileInfo.content, path)
        val regularExports = extractNodes(rootNode, fileInfo.content, path, fileLevelDependencies)
        val nodes = reexports + regularExports
        val dependencyOnThisFile = Dependency(path, isWildcard = true)
        val allNodes = nodes + extractDefaultExport(rootNode, path)

        return FileReport(nodes = allNodes.map { it.copy(dependencies = it.dependencies + dependencyOnThisFile) })
    }

    private fun extractReexports(
        rootNode: TSNode,
        fileBody: String,
        filePath: Path
    ): List<Node> {
        val namedReexports = indexTsExportQuery.execute(rootNode, fileBody, filePath).map { (export, dependencies) ->
            Node(
                pathWithName = filePath + (export.alias ?: export.identifier),
                physicalPath = fileInfo.physicalPath,
                language = fileInfo.language,
                nodeType = NodeType.REEXPORT,
                dependencies = dependencies,
                usedTypes = setOf(Type.simple(export.identifier))
            )
        }

        val wildcardReexports = extractWildcardReexports(rootNode, fileBody, filePath)

        return namedReexports + wildcardReexports
    }

    private fun extractWildcardReexports(
        rootNode: TSNode,
        fileBody: String,
        filePath: Path
    ): List<Node> {
        val wildcardSources = wildcardExportQuery.execute(rootNode, fileBody)

        // If no analysisRoot, can't resolve - skip
        if (fileInfo.analysisRoot == null) {
            return emptyList()
        }

        val extractor = TypescriptExportNameExtractor(fileInfo.analysisRoot)

        return wildcardSources.flatMap { sourceString ->
            val trimmedSource = sourceString.trim('"', '\'').stripSourceFileExtension()
            val sourcePath = resolveWildcardSourcePath(trimmedSource, filePath)
            val exportSource = extractor.extractExports(sourcePath)

            exportSource.names.map { exportName ->
                Node(
                    pathWithName = filePath + exportName,
                    physicalPath = fileInfo.physicalPath,
                    language = fileInfo.language,
                    nodeType = NodeType.REEXPORT,
                    dependencies = setOf(createDependencyForExport(sourcePath, exportName, exportSource.isIndexFile)),
                    usedTypes = setOf(Type.simple(exportName))
                )
            }
        }
    }

    private fun createDependencyForExport(
        sourcePath: Path,
        exportName: String,
        isIndexFile: Boolean
    ): Dependency {
        return if (isIndexFile) {
            // Target is index.ts: create dependency to src.common.constants.index.FOO
            Dependency(sourcePath + "index" + exportName)
        } else {
            // Target is direct file: create dependency to src.common.constants.FOO
            Dependency(sourcePath + exportName)
        }
    }

    private fun resolveWildcardSourcePath(
        sourceString: String,
        currentPath: Path
    ): Path {
        // Try tsconfig path resolution first
        val import = sourceString.toImport()

        if (import is DirectImport && fileInfo.analysisRoot != null) {
            val sourceFile = fileInfo.analysisRoot.resolve(fileInfo.physicalPath)
            val tsConfigResult = TsConfigResolver().findTsConfig(sourceFile)

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
        }

        // Fall back to relative resolution
        return resolveRelativePath(import, currentPath)
    }

    private fun extractDefaultExport(
        rootNode: TSNode,
        path: Path
    ): List<Node> {
        val defaultExport = defaultExportQuery.execute(rootNode, fileInfo.content)
        return if (defaultExport != null) {
            val nodeName = (path + DEFAULT_EXPORT_NODE_NAME).withUnderscores()
            listOf(
                Node(
                    pathWithName = path + nodeName,
                    physicalPath = fileInfo.physicalPath,
                    language = fileInfo.language,
                    nodeType = NodeType.REEXPORT,
                    dependencies = emptySet(),
                    usedTypes = setOf(Type.simple(defaultExport))
                )
            )
        } else {
            emptyList()
        }
    }

    private fun extractNodes(
        rootNode: TSNode,
        nodeBody: String,
        path: Path,
        dependenciesAndAliases: DependenciesAndAliases,
    ) = declarationsQuery.execute(rootNode, nodeBody).map {
        extractNodeFromDeclaration(it, nodeBody, path, dependenciesAndAliases)
    }

    private fun extractNodeFromDeclaration(
        declaration: Declaration,
        nodeBody: String,
        path: Path,
        dependenciesAndAliases: DependenciesAndAliases
    ): Node {
        val declarationBody = nodeAsString(declaration.node, nodeBody)
        val declarationNode = parseCode(declarationBody)
        val usedTypesFromDeclaration = getUsedTypes(declarationNode, declarationBody, dependenciesAndAliases)
            .filter { it != declaration.name }
            .map { Type.simple(it) }
            .toSet()

        // Merge types from imports with types used in declaration body
        val allUsedTypes = dependenciesAndAliases.usedTypes + usedTypesFromDeclaration

        return Node(
            pathWithName = path + declaration.name,
            physicalPath = fileInfo.physicalPath,
            language = fileInfo.language,
            nodeType = declaration.type,
            dependencies = dependenciesAndAliases.dependencies,
            usedTypes = allUsedTypes
        )
    }

    private fun parseCode(typescriptCode: String): TSNode {
        val parser = TSParser()
        parser.language = grammar
        val tree = parser.parseString(null, typescriptCode)
        return tree.rootNode
    }

    private fun getUsedTypes(
        declarationNode: TSNode,
        declarationBody: String,
        dependenciesAndAliases: DependenciesAndAliases
    ): List<String> {
        val typeIdentifiers = typeIdentifierQuery.execute(declarationNode, declarationBody).resolveAliases(dependenciesAndAliases)
        val constructorCalls = constructorCallQuery.execute(declarationNode, declarationBody).resolveAliases(dependenciesAndAliases)
        val memberAccesses = memberExpressionQuery.execute(declarationNode, declarationBody).resolveAliases(dependenciesAndAliases)
        val extensions = extendsClauseQuery.execute(declarationNode, declarationBody).resolveAliases(dependenciesAndAliases)
        val methodCalls = methodCallIdentifierQuery.execute(declarationNode, declarationBody).resolveAliases(dependenciesAndAliases)
        val identifiers = identifierQuery.execute(declarationNode, declarationBody).resolveAliases(dependenciesAndAliases)

        val allIdentifiers = dependenciesAndAliases.getAllKnownIdentifiers()
        val relevantIdentifiers = identifiers.filter { allIdentifiers.contains(it) }
        return typeIdentifiers + constructorCalls + memberAccesses + methodCalls + extensions + relevantIdentifiers
    }

    private fun List<String>.resolveAliases(dependenciesAndAliases: DependenciesAndAliases) =
        map { dependenciesAndAliases.importByAlias[it] ?: it }
}
