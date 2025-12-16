package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.model.Declaration
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.model.DependenciesAndAliases
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.queries.*
import de.maibornwolff.dependacharta.pipeline.analysis.model.*
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterTypescript

const val DEFAULT_EXPORT_NODE_NAME = "DEFAULT_EXPORT"

class TypescriptAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    private val isTsxFile = fileInfo.physicalPath.endsWith(".tsx")
    private val typescript = TreeSitterTypescript()
    private val declarationsQuery = TypescriptDeclarationsQuery(typescript)
    private val defaultExportQuery = TypescriptDefaultExportQuery(typescript)
    private val importStatementQuery = TypescriptImportStatementQuery(typescript)
    private val indexTsExportQuery = TypescriptIndexTsExportQuery(typescript)
    private val typeIdentifierQuery = TypescriptTypeIdentifierQuery(typescript)
    private val constructorCallQuery = TypescriptConstructorCallQuery(typescript)
    private val memberExpressionQuery = TypescriptMemberExpressionQuery(typescript)
    private val extendsClauseQuery = TypescriptExtendsClauseQuery(typescript)
    private val methodCallIdentifierQuery = TypescriptMethodCallIdentifierQuery(typescript)
    private val identifierQuery = TypescriptIdentifierQuery(typescript)

    override fun analyze(): FileReport {
        val rootNode = parseCode(fileInfo.content)

        val pathFromInfo = fileInfo.physicalPathAsPath()
        val fileName = pathFromInfo.getName()
        val fileSuffix = if (isTsxFile) "tsx" else "ts"
        val path = pathFromInfo.withoutFileSuffix(fileSuffix)

        val nodes = if (fileName == "index.ts" || fileName == "index.tsx") {
            // Index files need to process BOTH re-exports AND regular exports
            val fileLevelDependencies = importStatementQuery.execute(rootNode, fileInfo.content, path, fileInfo)
            val reexports = extractExportsOfIndexTs(rootNode, fileInfo.content, path)
            val regularExports = extractNodes(rootNode, fileInfo.content, path, fileLevelDependencies)
            reexports + regularExports
        } else {
            val fileLevelDependencies = importStatementQuery.execute(rootNode, fileInfo.content, path, fileInfo)
            extractNodes(rootNode, fileInfo.content, path, fileLevelDependencies)
        }
        val dependencyOnThisFile = Dependency(path, isWildcard = true)
        val allNodes = nodes + extractDefaultExport(rootNode, path)

        return FileReport(nodes = allNodes.map { it.copy(dependencies = it.dependencies + dependencyOnThisFile) })
    }

    private fun extractExportsOfIndexTs(
        rootNode: TSNode,
        fileBody: String,
        filePath: Path
    ) = indexTsExportQuery.execute(rootNode, fileBody, filePath).map { (export, dependencies) ->
        Node(
            pathWithName = filePath + (export.alias ?: export.identifier),
            physicalPath = fileInfo.physicalPath,
            language = fileInfo.language,
            nodeType = NodeType.VARIABLE,
            dependencies = dependencies,
            usedTypes = setOf(Type.simple(export.identifier))
        )
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
        val usedTypes = getUsedTypes(declarationNode, declarationBody, dependenciesAndAliases)
        return Node(
            pathWithName = path + declaration.name,
            physicalPath = fileInfo.physicalPath,
            language = fileInfo.language,
            nodeType = declaration.type,
            dependencies = dependenciesAndAliases.dependencies,
            usedTypes = usedTypes
                .filter { it != declaration.name }
                .map { Type.simple(it) }
                .toSet()
        )
    }

    private fun parseCode(typescriptCode: String): TSNode {
        val parser = TSParser()
        parser.language = typescript
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
