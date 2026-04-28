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
        val nodes = result.declarations.map { declaration ->
            val selectedImports = selectImports(declaration, result.imports)
            val importDeps = selectedImports.flatMap { convertImport(it) }.toSet()
            val packageDep = if (result.packagePath.isNotEmpty()) {
                setOf(Dependency(path = Path(result.packagePath), isWildcard = true))
            } else {
                emptySet()
            }
            val dependencies = importDeps + packageDep + extraDependencies(declaration)
            toNode(result.packagePath, dependencies, declaration, selectedImports)
        }
        return FileReport(nodes)
    }

    protected open fun selectImports(
        declaration: Declaration,
        imports: List<ImportDeclaration>
    ): List<ImportDeclaration> = imports

    protected open fun convertImport(import: ImportDeclaration): Set<Dependency> = setOf(import.toDependency())

    protected fun resolveImportPath(tsePath: List<String>): List<String> = resolveImportPath(tsePath, fileInfo)

    protected open fun buildPathWithName(
        packagePath: List<String>,
        declaration: Declaration,
    ): Path {
        return Path(packagePath + declaration.name)
    }

    protected open fun extraUsedTypes(imports: List<ImportDeclaration>): Set<Type> = emptySet()

    protected open fun extraDependencies(declaration: Declaration): Set<Dependency> = emptySet()

    private fun toNode(
        packagePath: List<String>,
        dependencies: Set<Dependency>,
        declaration: Declaration,
        selectedImports: List<ImportDeclaration>,
    ): Node {
        return Node(
            pathWithName = buildPathWithName(packagePath, declaration),
            physicalPath = fileInfo.physicalPath,
            language = language,
            nodeType = declaration.type.toNodeType(),
            dependencies = dependencies,
            usedTypes = declaration.usedTypes.map { it.toType() }.toSet() + extraUsedTypes(selectedImports)
        )
    }
}
