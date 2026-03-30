package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.toDependency
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.toNodeType
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.toType
import de.maibornwolff.dependacharta.pipeline.analysis.model.*
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.treesitter.excavationsite.api.Declaration
import de.maibornwolff.treesitter.excavationsite.api.Language
import de.maibornwolff.treesitter.excavationsite.api.TreeSitterDependencies

class KotlinAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    override fun analyze(): FileReport {
        val result = TreeSitterDependencies.analyze(fileInfo.content, Language.KOTLIN)
        val imports = result.imports.map { it.toDependency() }
        val dependencies = if (result.packagePath.isNotEmpty()) {
            (imports + Dependency(path = Path(result.packagePath), isWildcard = true)).toSet()
        } else {
            imports.toSet()
        }
        val nodes = result.declarations.map { declaration ->
            toNode(result.packagePath, dependencies, declaration)
        }
        return FileReport(nodes)
    }

    private fun toNode(
        packagePath: List<String>,
        dependencies: Set<Dependency>,
        declaration: Declaration,
    ): Node {
        return Node(
            pathWithName = Path(packagePath + declaration.parentPath + declaration.name),
            physicalPath = fileInfo.physicalPath,
            language = SupportedLanguage.KOTLIN,
            nodeType = declaration.type.toNodeType(),
            dependencies = dependencies,
            usedTypes = declaration.usedTypes.map { it.toType() }.toSet()
        )
    }
}
