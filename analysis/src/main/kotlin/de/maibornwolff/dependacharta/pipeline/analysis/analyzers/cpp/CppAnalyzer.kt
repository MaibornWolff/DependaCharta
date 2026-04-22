package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.RelativeImport
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.resolveRelativePath
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.toDependency
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.toNodeType
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.toType
import de.maibornwolff.dependacharta.pipeline.analysis.common.splitNameToParts
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.treesitter.excavationsite.api.ImportDeclaration
import de.maibornwolff.treesitter.excavationsite.api.ImportKind
import de.maibornwolff.treesitter.excavationsite.api.Language
import de.maibornwolff.treesitter.excavationsite.api.TreeSitterDependencies

class CppAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    override fun analyze(): FileReport {
        val result = TreeSitterDependencies.analyze(fileInfo.content, Language.CPP)
        val physicalPathAsPath = Path.fromPhysicalPath(fileInfo.physicalPath)
        val physicalPathParts = splitNameToParts(fileInfo.physicalPath).filter { it != "." }
        val globalImports = result.imports
            .filter { it.namespacePath.isEmpty() }
            .map { it.normalize(physicalPathAsPath) }

        val nodes = result.declarations.map { declaration ->
            val scopedImports = result.imports
                .filter { it.namespacePath == declaration.parentPath }
                .map { it.normalize(physicalPathAsPath) }
            val selfWildcard = if (declaration.parentPath.isNotEmpty()) {
                listOf(Dependency(path = Path(declaration.parentPath), isWildcard = true))
            } else {
                emptyList()
            }
            val namespacePrefixWildcards = declaration.usedTypes
                .map { it.namespacePrefix }
                .filter { it.isNotEmpty() }
                .map { Dependency(path = Path(it), isWildcard = true) }
                .toSet()
            val dependencies = (globalImports + scopedImports + selfWildcard + namespacePrefixWildcards).toSet()
            val pathWithName = if (declaration.parentPath.isEmpty()) {
                Path(physicalPathParts + declaration.name)
            } else {
                Path(declaration.parentPath + declaration.name)
            }

            Node(
                pathWithName = pathWithName,
                physicalPath = fileInfo.physicalPath,
                language = SupportedLanguage.CPP,
                nodeType = declaration.type.toNodeType(),
                dependencies = dependencies,
                usedTypes = declaration.usedTypes.map { it.toType() }.toSet(),
            )
        }
        return FileReport(nodes)
    }

    private fun ImportDeclaration.normalize(physicalPath: Path): Dependency {
        if (kind != ImportKind.INCLUDE) return toDependency()
        val firstSegment = path.firstOrNull()
        val resolved = if (firstSegment == "." || firstSegment == "..") {
            resolveRelativePath(RelativeImport(path.joinToString("/")), physicalPath)
        } else {
            Path(path)
        }
        val transformedFilename = resolved.parts.last().replace(".", "_")
        return Dependency(Path(resolved.parts.dropLast(1) + transformedFilename), isWildcard = false)
    }
}
