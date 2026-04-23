package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.vue

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.resolveImportPath
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.vue.queries.ScriptBlock
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.vue.queries.VueScriptExtractorQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.vue.queries.VueTemplateComponentUsageQuery
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.treesitter.excavationsite.api.Language
import de.maibornwolff.treesitter.excavationsite.api.TreeSitterDependencies
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterVue

class VueAnalyzer(
    private val fileInfo: FileInfo,
) : LanguageAnalyzer {
    private val vue = TreeSitterVue()
    private val scriptExtractorQuery = VueScriptExtractorQuery(vue)
    private val templateComponentUsageQuery = VueTemplateComponentUsageQuery(vue)

    override fun analyze(): FileReport {
        val rootNode = parseCode(fileInfo.content)
        val componentPath = fileInfo.physicalPathAsPath().withoutFileSuffix("vue")

        val scriptBlock = scriptExtractorQuery.execute(rootNode, fileInfo.content)
        val templateComponents = templateComponentUsageQuery.execute(rootNode, fileInfo.content)

        val scriptDependencies = if (scriptBlock != null && scriptBlock.content.isNotEmpty()) {
            extractScriptDependencies(scriptBlock)
        } else {
            emptySet()
        }

        val templateDependencies = templateComponents
            .map { componentName ->
                Dependency(path = Path.fromStringWithDots(componentName))
            }.toSet()

        val templateUsedTypes = templateComponents.map { Type.simple(it) }.toSet()

        val componentNode = Node(
            pathWithName = componentPath,
            physicalPath = fileInfo.physicalPath,
            language = fileInfo.language,
            nodeType = NodeType.CLASS,
            dependencies = scriptDependencies + templateDependencies,
            usedTypes = templateUsedTypes,
        )

        return FileReport(nodes = listOf(componentNode))
    }

    private fun extractScriptDependencies(scriptBlock: ScriptBlock): Set<Dependency> {
        val tseLanguage = tseLanguageFor(scriptBlock.lang)
        return TreeSitterDependencies
            .analyze(scriptBlock.content, tseLanguage)
            .imports
            .map { import ->
                val resolvedPath = resolveImportPath(import.path, fileInfo)
                Dependency(path = Path(resolvedPath), isWildcard = import.isWildcard)
            }.toSet()
    }

    private fun tseLanguageFor(lang: String?): Language {
        return when (lang?.lowercase()) {
            "ts" -> Language.TYPESCRIPT
            "tsx" -> Language.TSX
            else -> Language.JAVASCRIPT
        }
    }

    private fun parseCode(vueCode: String): TSNode {
        val parser = TSParser()
        parser.language = vue
        val tree = parser.parseString(null, vueCode)
        return tree.rootNode
    }
}
