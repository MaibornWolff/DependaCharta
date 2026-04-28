package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.vue

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.resolveImportPath
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.stripSourceFileExtension
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

// "DEFAULT_EXPORT" is the name TSE assigns to default export declarations
private const val DEFAULT_EXPORT_SENTINEL = "DEFAULT_EXPORT"

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

        val (scriptDependencies, scriptUsedTypes) = if (scriptBlock != null && scriptBlock.content.isNotEmpty()) {
            analyzeScript(scriptBlock)
        } else {
            Pair(emptySet(), emptySet())
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
            usedTypes = templateUsedTypes + scriptUsedTypes,
        )

        return FileReport(nodes = listOf(componentNode))
    }

    private fun analyzeScript(scriptBlock: ScriptBlock): Pair<Set<Dependency>, Set<Type>> {
        val tseLanguage = tseLanguageFor(scriptBlock.lang)
        val tseResult = TreeSitterDependencies.analyze(scriptBlock.content, tseLanguage)
        val dependencies = tseResult.imports
            .map { import ->
                val resolvedPath = resolveImportPath(import.path, fileInfo)
                Dependency(path = Path(resolvedPath), isWildcard = import.isWildcard)
            }.toSet()
        val usedTypes = tseResult.imports
            .filter { !it.isWildcard && it.path.isNotEmpty() }
            .mapNotNull { import ->
                val specifier = import.path.last().stripSourceFileExtension()
                if (specifier.isEmpty() || specifier == DEFAULT_EXPORT_SENTINEL) null else Type.simple(specifier)
            }.toSet()
        return Pair(dependencies, usedTypes)
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
