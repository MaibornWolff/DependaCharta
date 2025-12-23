package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries.JavascriptCommonJsExportsQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries.JavascriptCommonJsRequireQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries.JavascriptDeclarationsQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries.JavascriptEs6ExportsQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries.JavascriptEs6ImportsQuery
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.analysis.model.TypeOfUsage
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterJavascript

const val DEFAULT_EXPORT_NAME = "default"

private const val INDEX_JS = "index.js"
private const val INDEX_JSX = "index.jsx"

class JavascriptAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    private val javascript = TreeSitterJavascript()
    private val declarationsQuery = JavascriptDeclarationsQuery(javascript)
    private val es6ImportsQuery = JavascriptEs6ImportsQuery(javascript)
    private val es6ExportsQuery = JavascriptEs6ExportsQuery(javascript)
    private val commonJsRequireQuery = JavascriptCommonJsRequireQuery(javascript)
    private val commonJsExportsQuery = JavascriptCommonJsExportsQuery(javascript)

    override fun analyze(): FileReport {
        val rootNode = parseCode(fileInfo.content)
        val filePathWithSlashes = fileInfo.physicalPathAsPath()
        val pathWithoutFileEnding = filePathWithSlashes.withoutFileSuffix(if (fileInfo.physicalPath.endsWith(".jsx")) "jsx" else "js")

        val fileName = filePathWithSlashes.getName()
        val isIndexFile = fileName == INDEX_JS || fileName == INDEX_JSX

        if (isIndexFile) {
            return analyzeIndexFile(rootNode, pathWithoutFileEnding)
        }

        return analyzeRegularFile(rootNode, pathWithoutFileEnding)
    }

    private fun analyzeIndexFile(
        rootNode: TSNode,
        filePath: Path
    ): FileReport {
        val reexports = extractReexports(rootNode, filePath)
        val regularExports = extractRegularExports(rootNode, filePath)
        return FileReport(reexports + regularExports)
    }

    private fun analyzeRegularFile(
        rootNode: TSNode,
        filePath: Path
    ): FileReport {
        val nodes = extractRegularExports(rootNode, filePath)
        return FileReport(nodes)
    }

    private fun extractReexports(
        rootNode: TSNode,
        filePath: Path
    ): List<Node> {
        val es6Reexports = es6ExportsQuery.executeReexports(rootNode, fileInfo.content, filePath)
        return es6Reexports.map { reexport ->
            Node(
                pathWithName = filePath + reexport.exportedName,
                physicalPath = fileInfo.physicalPath,
                language = fileInfo.language,
                nodeType = NodeType.REEXPORT,
                dependencies = setOf(reexport.dependency),
                usedTypes = emptySet()
            )
        }
    }

    private fun extractRegularExports(
        rootNode: TSNode,
        filePath: Path
    ): List<Node> {
        val es6Imports = es6ImportsQuery.execute(rootNode, fileInfo.content, filePath)
        val commonJsRequires = commonJsRequireQuery.execute(rootNode, fileInfo.content, filePath)
        val allImports = es6Imports + commonJsRequires

        val es6ExportsInfo = es6ExportsQuery.executeExports(rootNode, fileInfo.content)
        val commonJsExports = commonJsExportsQuery.execute(rootNode, fileInfo.content)
        val allExports = es6ExportsInfo.exports + commonJsExports

        val declarations = declarationsQuery.execute(rootNode, fileInfo.content)

        return buildNodesFromDeclarations(declarations, allExports, allImports, filePath, es6ExportsInfo.defaultExportedIdentifier)
    }

    private fun buildNodesFromDeclarations(
        declarations: List<JavascriptDeclaration>,
        exports: Set<String>,
        imports: List<Dependency>,
        filePath: Path,
        defaultExportedIdentifier: String?
    ): List<Node> {
        return declarations.filter { it.name in exports || it.name == defaultExportedIdentifier }.map { declaration ->
            val nodeName = if (declaration.name == defaultExportedIdentifier) {
                DEFAULT_EXPORT_NAME
            } else {
                declaration.name
            }

            Node(
                pathWithName = filePath + nodeName,
                physicalPath = fileInfo.physicalPath,
                language = fileInfo.language,
                nodeType = declaration.nodeType,
                dependencies = imports.toSet(),
                usedTypes = createSyntheticTypesFromImports(imports)
            )
        }
    }

    private fun createSyntheticTypesFromImports(imports: List<Dependency>): Set<Type> {
        return imports
            .mapNotNull { dependency ->
                val identifier = dependency.path.parts.lastOrNull()
                if (identifier != null && identifier.isNotEmpty()) {
                    Type.simple(identifier, TypeOfUsage.USAGE)
                } else {
                    null
                }
            }.toSet()
    }

    private fun parseCode(javascriptCode: String): TSNode {
        val parser = TSParser()
        parser.language = javascript
        val tree = parser.parseString(null, javascriptCode)
        return tree.rootNode
    }
}

data class JavascriptDeclaration(
    val name: String,
    val nodeType: NodeType
)

data class JavascriptReexport(
    val exportedName: String,
    val dependency: Dependency
)
