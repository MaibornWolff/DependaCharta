package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.vue

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries.JavascriptCommonJsRequireQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.queries.JavascriptEs6ImportsQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.model.DependenciesAndAliases
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.queries.TypescriptImportStatementQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.vue.queries.VueScriptExtractorQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.vue.queries.VueTemplateComponentUsageQuery
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.analysis.model.TypeOfUsage
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterJavascript
import org.treesitter.TreeSitterTypescript
import org.treesitter.TreeSitterVue

class VueAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    private val vue = TreeSitterVue()
    private val scriptExtractorQuery = VueScriptExtractorQuery(vue)
    private val templateComponentUsageQuery = VueTemplateComponentUsageQuery(vue)

    override fun analyze(): FileReport {
        val rootNode = parseCode(fileInfo.content)
        val pathFromInfo = fileInfo.physicalPathAsPath()
        val componentPath = pathFromInfo.withoutFileSuffix("vue")

        // Extract script block
        val scriptBlock = scriptExtractorQuery.execute(rootNode, fileInfo.content)

        // Extract template component usage
        val templateComponents = templateComponentUsageQuery.execute(rootNode, fileInfo.content)

        // Analyze script content if present
        val scriptImportsResult = if (scriptBlock != null && scriptBlock.content.isNotEmpty()) {
            analyzeScriptBlock(scriptBlock, componentPath)
        } else {
            DependenciesAndAliases(emptySet(), emptyMap(), emptySet())
        }

        // Create dependencies from template component usage
        val templateDependencies = templateComponents
            .map { componentName ->
                // Create dependency to potential component file
                val resolvedPath = resolveComponentPath(componentName)
                Dependency(
                    path = resolvedPath,
                    type = TypeOfUsage.USAGE
                )
            }.toSet()

        // Merge script and template dependencies
        val allDependencies = scriptImportsResult.dependencies + templateDependencies

        // Merge usedTypes from script imports with template component names
        val templateUsedTypes = templateComponents.map { Type.simple(it) }.toSet()
        val allUsedTypes = scriptImportsResult.usedTypes + templateUsedTypes

        // Create node for Vue component
        val componentNode = Node(
            pathWithName = componentPath,
            physicalPath = fileInfo.physicalPath,
            language = fileInfo.language,
            nodeType = NodeType.CLASS,
            dependencies = allDependencies,
            usedTypes = allUsedTypes
        )

        return FileReport(nodes = listOf(componentNode))
    }

    private fun analyzeScriptBlock(
        scriptBlock: de.maibornwolff.dependacharta.pipeline.analysis.analyzers.vue.queries.ScriptBlock,
        componentPath: Path
    ): DependenciesAndAliases {
        val lang = scriptBlock.lang ?: "js"

        // Create synthetic FileInfo for script content with appropriate extension
        val scriptFileInfo = when (lang.lowercase()) {
            "ts" -> FileInfo(
                language = SupportedLanguage.TYPESCRIPT,
                physicalPath = fileInfo.physicalPath.replace(".vue", ".ts"),
                content = scriptBlock.content,
                analysisRoot = fileInfo.analysisRoot
            )
            "tsx" -> FileInfo(
                language = SupportedLanguage.TYPESCRIPT,
                physicalPath = fileInfo.physicalPath.replace(".vue", ".tsx"),
                content = scriptBlock.content,
                analysisRoot = fileInfo.analysisRoot
            )
            "jsx" -> FileInfo(
                language = SupportedLanguage.JAVASCRIPT,
                physicalPath = fileInfo.physicalPath.replace(".vue", ".jsx"),
                content = scriptBlock.content,
                analysisRoot = fileInfo.analysisRoot
            )
            else -> FileInfo(
                language = SupportedLanguage.JAVASCRIPT,
                physicalPath = fileInfo.physicalPath.replace(".vue", ".js"),
                content = scriptBlock.content,
                analysisRoot = fileInfo.analysisRoot
            )
        }

        // Directly extract imports from script content using appropriate parser
        val scriptPath = scriptFileInfo.physicalPathAsPath().withoutFileSuffix(
            when (lang.lowercase()) {
                "tsx" -> "tsx"
                "ts" -> "ts"
                "jsx" -> "jsx"
                else -> "js"
            }
        )

        return when (lang.lowercase()) {
            "ts", "tsx" -> extractTypescriptImports(scriptBlock.content, scriptPath, scriptFileInfo)
            else -> extractJavascriptImports(scriptBlock.content, scriptPath, scriptFileInfo)
        }
    }

    private fun extractTypescriptImports(
        scriptContent: String,
        scriptPath: Path,
        scriptFileInfo: FileInfo
    ): DependenciesAndAliases {
        val typescript = TreeSitterTypescript()
        val parser = TSParser()
        parser.language = typescript
        val tree = parser.parseString(null, scriptContent)
        val rootNode = tree.rootNode

        val importQuery = TypescriptImportStatementQuery(typescript)
        return importQuery.execute(rootNode, scriptContent, scriptPath, scriptFileInfo)
    }

    private fun extractJavascriptImports(
        scriptContent: String,
        scriptPath: Path,
        scriptFileInfo: FileInfo
    ): DependenciesAndAliases {
        val javascript = TreeSitterJavascript()
        val parser = TSParser()
        parser.language = javascript
        val tree = parser.parseString(null, scriptContent)
        val rootNode = tree.rootNode

        val es6ImportsQuery = JavascriptEs6ImportsQuery(javascript)
        val commonJsRequireQuery = JavascriptCommonJsRequireQuery(javascript)

        val es6ImportsResult = es6ImportsQuery.execute(rootNode, scriptContent, scriptPath, scriptFileInfo)
        val commonJsRequires = commonJsRequireQuery.execute(rootNode, scriptContent, scriptPath)

        return DependenciesAndAliases(
            dependencies = es6ImportsResult.dependencies + commonJsRequires,
            importByAlias = es6ImportsResult.importByAlias,
            usedTypes = es6ImportsResult.usedTypes
        )
    }

    private fun resolveComponentPath(componentName: String): Path {
        // Simple resolution: assume component is in same directory or a common location
        // This creates a path that can be resolved later during dependency resolution
        return Path.fromStringWithDots(componentName)
    }

    private fun parseCode(vueCode: String): TSNode {
        val parser = TSParser()
        parser.language = vue
        val tree = parser.parseString(null, vueCode)
        return tree.rootNode
    }
}
