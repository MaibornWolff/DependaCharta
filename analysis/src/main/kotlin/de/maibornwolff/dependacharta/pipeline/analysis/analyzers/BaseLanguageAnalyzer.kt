package de.maibornwolff.dependacharta.pipeline.analysis.analyzers

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.resolveImportPath
import de.maibornwolff.dependacharta.pipeline.analysis.model.*
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.treesitter.excavationsite.api.Declaration
import de.maibornwolff.treesitter.excavationsite.api.ImportDeclaration
import de.maibornwolff.treesitter.excavationsite.api.Language
import de.maibornwolff.treesitter.excavationsite.api.TreeSitterDependencies

abstract class BaseLanguageAnalyzer(
    protected val fileInfo: FileInfo
) : LanguageAnalyzer {
    protected abstract val language: SupportedLanguage

    open fun tseLanguage(): Language = Language.valueOf(language.name)

    override fun analyze(): FileReport {
        val result = TreeSitterDependencies.analyze(fileInfo.content, tseLanguage())
        val imports = result.imports.map { convertImport(it) }
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

    protected open fun convertImport(import: ImportDeclaration): Dependency = import.toDependency()

    protected fun resolveImportPath(tsePath: List<String>): List<String> = resolveImportPath(tsePath, fileInfo)

    protected open fun buildPathWithName(
        packagePath: List<String>,
        declaration: Declaration,
    ): Path {
        return Path(packagePath + declaration.name)
    }

    private fun toNode(
        packagePath: List<String>,
        dependencies: Set<Dependency>,
        declaration: Declaration,
    ): Node {
        return Node(
            pathWithName = buildPathWithName(packagePath, declaration),
            physicalPath = fileInfo.physicalPath,
            language = language,
            nodeType = declaration.type.toNodeType(),
            dependencies = dependencies,
            usedTypes = declaration.usedTypes.map { it.toType() }.toSet()
        )
    }
}
