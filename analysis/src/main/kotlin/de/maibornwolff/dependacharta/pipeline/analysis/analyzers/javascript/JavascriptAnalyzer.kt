package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.BaseLanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.stripSourceFileExtension
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.treesitter.excavationsite.api.Declaration
import de.maibornwolff.treesitter.excavationsite.api.ImportDeclaration

const val DEFAULT_EXPORT_NAME = "default"

class JavascriptAnalyzer(
    private val fileInfo: FileInfo,
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

    private fun resolveImportPath(tsePath: List<String>): List<String> {
        val stripped = tsePath.map { it.stripSourceFileExtension() }.filter { it.isNotEmpty() }
        if (stripped.isEmpty() || (stripped.first() != "." && stripped.first() != "..")) return stripped
        var dirParts = fileInfo.physicalPathAsPath().parts.dropLast(1)
        var remaining = stripped
        while (remaining.isNotEmpty() && (remaining.first() == "." || remaining.first() == "..")) {
            if (remaining.first() == "..") dirParts = dirParts.dropLast(1)
            remaining = remaining.drop(1)
        }
        return dirParts + remaining
    }
}

// Kept for query classes (deleted in cleanup phase)
data class JavascriptDeclaration(
    val name: String,
    val nodeType: NodeType
)

data class JavascriptReexport(
    val exportedName: String,
    val dependency: Dependency
)
