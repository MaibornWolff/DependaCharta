package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.BaseLanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.TSE_DEFAULT_EXPORT_NAME
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.defaultImportPath
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.treesitter.excavationsite.api.Declaration
import de.maibornwolff.treesitter.excavationsite.api.ImportDeclaration

private const val JS_DEFAULT_EXPORT = "default"

// Unlike TypescriptAnalyzer, JavascriptAnalyzer does not expand wildcard re-exports
// (`export * from './module'`) into one node per re-exported name. A JS wildcard re-export is
// represented as a single REEXPORT node carrying the wildcard dependency. This matches DC's
// established JavaScript behavior (see the `should handle ES6 wildcard re-exports` test).
class JavascriptAnalyzer(
    fileInfo: FileInfo,
) : BaseLanguageAnalyzer(fileInfo) {
    override val language = SupportedLanguage.JAVASCRIPT

    override fun buildPathWithName(
        packagePath: List<String>,
        declaration: Declaration,
    ): Path {
        val extension = if (fileInfo.physicalPath.endsWith(".jsx")) "jsx" else "js"
        return fileInfo.physicalPathAsPath().withoutFileSuffix(extension) + localExportName(declaration.name)
    }

    override fun convertImport(import: ImportDeclaration): Set<Dependency> {
        val path = resolveImportPath(import.defaultImportPath())
        val resolvedPath = if (path.isEmpty()) path else path.dropLast(1) + localExportName(path.last())
        return setOf(Dependency(path = Path(resolvedPath), isWildcard = import.isWildcard))
    }

    // TSE marks the default export/import with a sentinel name; JavaScript represents the default
    // binding as `default` in both node paths and dependency paths.
    private fun localExportName(tseName: String): String {
        return if (tseName == TSE_DEFAULT_EXPORT_NAME) JS_DEFAULT_EXPORT else tseName
    }
}
