package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.queries.AblClassNameQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.queries.AblDeclarationsQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.queries.AblIncludeQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.queries.AblInheritanceQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.queries.AblRunQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.queries.AblUsingQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterAbl

class AblAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    private val abl = TreeSitterAbl()
    private val declarationsQuery = AblDeclarationsQuery(abl)
    private val classNameQuery = AblClassNameQuery(abl)
    private val usingQuery = AblUsingQuery(abl)
    private val runQuery = AblRunQuery(abl)
    private val includeQuery = AblIncludeQuery(abl)
    private val inheritanceQuery = AblInheritanceQuery(abl)

    override fun analyze(): FileReport {
        val rootNode = parseCode(fileInfo.content)

        val classDefinitions = declarationsQuery.execute(rootNode)

        if (classDefinitions.isNotEmpty()) {
            return analyzeClassFile(rootNode, classDefinitions)
        }

        return analyzeProceduralFile(rootNode)
    }

    private fun analyzeClassFile(
        rootNode: TSNode,
        classDefinitions: List<TSNode>
    ): FileReport {
        val usingDependencies = usingQuery.execute(rootNode, fileInfo.content)
        val includeDependencies = includeQuery.execute(rootNode, fileInfo.content)
        val allDependencies = (usingDependencies + includeDependencies).toSet()

        // Convert USING dependencies to usedTypes for resolution (skip wildcards)
        val usingUsedTypes = usingDependencies
            .filter { !it.isWildcard }
            .map { dep -> Type.simple(dep.path.parts.last()) }
            .toSet()

        // Convert include dependencies to usedTypes for resolution
        val includeUsedTypes = includeDependencies
            .map { dep -> Type.simple(dep.path.withDots()) }
            .toSet()

        val nodes = classDefinitions.map { classNode ->
            val classPath = classNameQuery.execute(classNode, fileInfo.content)
                ?: fileInfo.physicalPathAsPath()
            val inheritedTypes = inheritanceQuery.execute(classNode, fileInfo.content)

            Node(
                pathWithName = classPath,
                physicalPath = fileInfo.physicalPath,
                language = SupportedLanguage.ABL,
                nodeType = NodeType.CLASS,
                dependencies = allDependencies,
                usedTypes = inheritedTypes + usingUsedTypes + includeUsedTypes
            )
        }

        return FileReport(nodes)
    }

    private fun analyzeProceduralFile(rootNode: TSNode): FileReport {
        val filePath = fileInfo.physicalPathAsPath()
        val extension = fileInfo.physicalPath.substringAfterLast(".")
        val pathWithoutExtension = filePath.withoutFileSuffix(extension)

        val usingDependencies = usingQuery.execute(rootNode, fileInfo.content)
        val runDependencies = runQuery.execute(rootNode, fileInfo.content)
        val includeDependencies = includeQuery.execute(rootNode, fileInfo.content)

        val allDependencies = (usingDependencies + runDependencies + includeDependencies).toSet()

        // Convert USING dependencies to usedTypes for resolution (skip wildcards)
        val usingUsedTypes = usingDependencies
            .filter { !it.isWildcard }
            .map { dep -> Type.simple(dep.path.parts.last()) }
            .toSet()

        // Convert direct dependencies (RUN and include) to usedTypes for resolution
        val directUsedTypes = (runDependencies + includeDependencies)
            .map { dep -> Type.simple(dep.path.withDots()) }
            .toSet()

        val node = Node(
            pathWithName = pathWithoutExtension,
            physicalPath = fileInfo.physicalPath,
            language = SupportedLanguage.ABL,
            nodeType = NodeType.CLASS,
            dependencies = allDependencies,
            usedTypes = usingUsedTypes + directUsedTypes
        )

        return FileReport(listOf(node))
    }

    private fun parseCode(ablCode: String): TSNode {
        val parser = TSParser()
        parser.language = abl
        val tree = parser.parseString(null, ablCode)
        return tree.rootNode
    }
}
