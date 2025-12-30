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
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterJavascript

const val DEFAULT_EXPORT_NAME = "default"

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
        val fileSuffix = if (fileInfo.physicalPath.endsWith(".jsx")) "jsx" else "js"
        val pathWithoutFileEnding = filePathWithSlashes.withoutFileSuffix(fileSuffix)

        val reexports = extractReexports(rootNode, pathWithoutFileEnding)
        val regularExports = extractRegularExports(rootNode, pathWithoutFileEnding)
        return FileReport(reexports + regularExports)
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
        val es6ImportsResult = es6ImportsQuery.execute(rootNode, fileInfo.content, filePath, fileInfo)
        val commonJsRequires = commonJsRequireQuery.execute(rootNode, fileInfo.content, filePath)
        val allDependencies = es6ImportsResult.dependencies + commonJsRequires

        val es6ExportsInfo = es6ExportsQuery.executeExports(rootNode, fileInfo.content)
        val commonJsExports = commonJsExportsQuery.execute(rootNode, fileInfo.content)
        val allExports = es6ExportsInfo.exports + commonJsExports

        val declarations = declarationsQuery.execute(rootNode, fileInfo.content)

        return buildNodesFromDeclarations(
            declarations,
            allExports,
            allDependencies,
            es6ImportsResult.usedTypes,
            filePath,
            es6ExportsInfo.defaultExportedIdentifier
        )
    }

    private fun buildNodesFromDeclarations(
        declarations: List<JavascriptDeclaration>,
        exports: Set<String>,
        dependencies: Set<Dependency>,
        usedTypes: Set<Type>,
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
                dependencies = dependencies,
                usedTypes = usedTypes
            )
        }
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
