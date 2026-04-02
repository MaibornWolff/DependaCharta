package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.toDependency
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.toNodeType
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.toType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.treesitter.excavationsite.api.Language
import de.maibornwolff.treesitter.excavationsite.api.TreeSitterDependencies

class CSharpAnalyzer(
    private val fileInfo: FileInfo,
) : LanguageAnalyzer {
    override fun analyze(): FileReport {
        val result = TreeSitterDependencies.analyze(fileInfo.content, Language.CSHARP)
        val imports = result.imports.map { it.toDependency() }

        val nodes = result.declarations.map { declaration ->
            val namespacePath = declaration.parentPath
            val dependencies = (imports + Dependency(path = Path(namespacePath), isWildcard = true)).toSet()

            Node(
                pathWithName = Path(namespacePath + declaration.name),
                physicalPath = fileInfo.physicalPath,
                language = SupportedLanguage.C_SHARP,
                nodeType = declaration.type.toNodeType(),
                dependencies = dependencies,
                usedTypes = declaration.usedTypes.map { it.toType() }.toSet(),
            )
        }
        return FileReport(nodes)
    }
}
