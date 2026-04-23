package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.BaseLanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.treesitter.excavationsite.api.Declaration
import de.maibornwolff.treesitter.excavationsite.api.ImportDeclaration

class JavascriptAnalyzer(
    fileInfo: FileInfo,
) : BaseLanguageAnalyzer(fileInfo) {
    override val language = SupportedLanguage.JAVASCRIPT

    override fun buildPathWithName(
        packagePath: List<String>,
        declaration: Declaration,
    ): Path {
        val extension = if (fileInfo.physicalPath.endsWith(".jsx")) "jsx" else "js"
        return fileInfo.physicalPathAsPath().withoutFileSuffix(extension) + declaration.name
    }

    override fun convertImport(import: ImportDeclaration): Dependency {
        val resolvedPath = resolveImportPath(import.path)
        return Dependency(path = Path(resolvedPath), isWildcard = import.isWildcard)
    }
}
