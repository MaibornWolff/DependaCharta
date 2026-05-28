package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.BaseLanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.TSE_DEFAULT_EXPORT_NAME
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.treesitter.excavationsite.api.Declaration
import de.maibornwolff.treesitter.excavationsite.api.ImportDeclaration

private const val JS_DEFAULT_EXPORT = "default"

class JavascriptAnalyzer(
    fileInfo: FileInfo,
) : BaseLanguageAnalyzer(fileInfo) {
    override val language = SupportedLanguage.JAVASCRIPT

    override fun buildPathWithName(
        packagePath: List<String>,
        declaration: Declaration,
    ): Path {
        val extension = if (fileInfo.physicalPath.endsWith(".jsx")) "jsx" else "js"
        val name = if (declaration.name == TSE_DEFAULT_EXPORT_NAME) JS_DEFAULT_EXPORT else declaration.name
        return fileInfo.physicalPathAsPath().withoutFileSuffix(extension) + name
    }

    override fun convertImport(import: ImportDeclaration): Set<Dependency> {
        val resolvedPath = resolveImportPath(import.path).let { path ->
            if (path.lastOrNull() == TSE_DEFAULT_EXPORT_NAME) path.dropLast(1) + JS_DEFAULT_EXPORT else path
        }
        return setOf(Dependency(path = Path(resolvedPath), isWildcard = import.isWildcard))
    }
}
