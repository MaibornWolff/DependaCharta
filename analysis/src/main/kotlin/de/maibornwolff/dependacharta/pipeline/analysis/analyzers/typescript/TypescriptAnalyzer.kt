package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.BaseLanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.stripSourceFileExtension
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.treesitter.excavationsite.api.Declaration
import de.maibornwolff.treesitter.excavationsite.api.DeclarationType
import de.maibornwolff.treesitter.excavationsite.api.ImportDeclaration
import de.maibornwolff.treesitter.excavationsite.api.Language

const val DEFAULT_EXPORT_NODE_NAME = "DEFAULT_EXPORT"

class TypescriptAnalyzer(
    fileInfo: FileInfo,
) : BaseLanguageAnalyzer(fileInfo) {
    override val language = SupportedLanguage.TYPESCRIPT

    override fun tseLanguage(): Language = if (fileInfo.physicalPath.endsWith(".tsx")) Language.TSX else Language.TYPESCRIPT

    override fun buildPathWithName(
        packagePath: List<String>,
        declaration: Declaration,
    ): Path {
        if (declaration.parentPath.isNotEmpty()) {
            return Path(declaration.parentPath) + declaration.name
        }
        val extension = if (fileInfo.physicalPath.endsWith(".tsx")) "tsx" else "ts"
        return fileInfo.physicalPathAsPath().withoutFileSuffix(extension) + declaration.name
    }

    override fun selectImports(
        declaration: Declaration,
        imports: List<ImportDeclaration>
    ): List<ImportDeclaration> {
        if (declaration.type != DeclarationType.REEXPORT) return imports
        val reexportNames = setOf(declaration.name) + declaration.usedTypes.map { it.name }
        return imports.filter { it.path.isNotEmpty() && it.path.last() in reexportNames }
    }

    override fun convertImport(import: ImportDeclaration): Set<Dependency> {
        val resolvedPath = resolveImportPath(import.path)
        val primary = Dependency(path = Path(resolvedPath), isWildcard = import.isWildcard)
        val index = buildIndexDependency(resolvedPath, import.isWildcard)
        return if (index != null) setOf(primary, index) else setOf(primary)
    }

    override fun extraUsedTypes(imports: List<ImportDeclaration>): Set<Type> {
        return imports
            .filter { !it.isWildcard && it.path.isNotEmpty() }
            .mapNotNull { import ->
                val specifier = import.path.last().stripSourceFileExtension()
                if (specifier.isEmpty() || specifier == DEFAULT_EXPORT_NODE_NAME) null else Type.simple(specifier)
            }.toSet()
    }

    private fun buildIndexDependency(
        resolvedPath: List<String>,
        isWildcard: Boolean
    ): Dependency? {
        if (isWildcard) {
            return if (resolvedPath.isEmpty()) {
                null
            } else {
                Dependency(Path(resolvedPath + "index"), isWildcard = true)
            }
        }
        return if (resolvedPath.size < 2) {
            null
        } else {
            Dependency(Path(resolvedPath.dropLast(1) + listOf("index") + resolvedPath.takeLast(1)))
        }
    }
}
