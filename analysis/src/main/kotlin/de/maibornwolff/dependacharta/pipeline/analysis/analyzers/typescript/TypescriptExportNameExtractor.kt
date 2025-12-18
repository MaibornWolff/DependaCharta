package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.toImport
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.resolveRelativePath
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.queries.TypescriptDeclarationsQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.queries.TypescriptIndexTsExportQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.queries.TypescriptWildcardExportQuery
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterTypescript
import java.io.File

/**
 * Information about discovered exports from a module.
 */
data class ExportSource(
    val names: Set<String>,
    val isIndexFile: Boolean
)

/**
 * Extracts export names from a TypeScript module.
 * Returns export names along with information about whether they come from an index file.
 */
class TypescriptExportNameExtractor(
    private val analysisRoot: File
) {
    private val typescript = TreeSitterTypescript()
    private val declarationsQuery = TypescriptDeclarationsQuery(typescript)
    private val indexTsExportQuery = TypescriptIndexTsExportQuery(typescript)
    private val wildcardExportQuery = TypescriptWildcardExportQuery(typescript)

    /**
     * Extracts all export names from a module, following wildcard re-exports.
     *
     * @param targetPath Module path (e.g., Path("src", "common", "constants"))
     * @param visitedPaths Paths already visited (for cycle detection)
     * @return ExportSource with names and file type information
     */
    fun extractExports(
        targetPath: Path,
        visitedPaths: Set<Path> = emptySet()
    ): ExportSource {
        // Guard: circular references
        if (visitedPaths.contains(targetPath)) {
            return ExportSource(emptySet(), false)
        }

        // Find and read the file
        val fileResult = findTargetFile(targetPath)
        if (fileResult == null) {
            return ExportSource(emptySet(), false)
        }

        val (targetFile, isIndexFile) = fileResult
        val fileContent = targetFile.readText(Charsets.UTF_8)

        // Parse the file
        val rootNode = parseCode(fileContent)

        // Extract direct exports
        val directExports = extractDirectExportNames(rootNode, fileContent, targetPath)

        // Extract wildcard re-export sources
        val wildcardSources = wildcardExportQuery.execute(rootNode, fileContent)

        // Recursively get exports from wildcard sources
        val wildcardExports = wildcardSources
            .flatMap { sourceString ->
                val trimmedSource = sourceString.trim('"', '\'').trimFileEnding()
                val sourcePath = resolveRelativePath(trimmedSource.toImport(), targetPath)
                extractExports(sourcePath, visitedPaths + targetPath).names
            }.toSet()

        return ExportSource(
            names = directExports + wildcardExports,
            isIndexFile = isIndexFile
        )
    }

    /**
     * Finds the target file and returns both the file and whether it's an index file.
     * @return Pair of (File, isIndexFile) or null if not found
     */
    private fun findTargetFile(targetPath: Path): Pair<File, Boolean>? {
        val relativePath = targetPath.parts.joinToString("/")

        // Try direct files first (.ts, .tsx)
        val directCandidates = listOf(
            "$relativePath.ts" to false,
            "$relativePath.tsx" to false
        )

        for ((path, isIndex) in directCandidates) {
            val file = analysisRoot.resolve(path)
            if (file.exists() && file.isFile) {
                return file to isIndex
            }
        }

        // Try index files (index.ts, index.tsx)
        val indexCandidates = listOf(
            "$relativePath/index.ts" to true,
            "$relativePath/index.tsx" to true
        )

        for ((path, isIndex) in indexCandidates) {
            val file = analysisRoot.resolve(path)
            if (file.exists() && file.isFile) {
                return file to isIndex
            }
        }

        return null
    }

    private fun parseCode(code: String): TSNode {
        val parser = TSParser()
        parser.language = typescript
        val tree = parser.parseString(null, code)
        return tree.rootNode
    }

    private fun extractDirectExportNames(
        rootNode: TSNode,
        fileBody: String,
        currentPath: Path
    ): Set<String> {
        // Get exported declarations (export const FOO, export class Bar, etc.)
        val declarations = declarationsQuery
            .execute(rootNode, fileBody)
            .map { it.name }
            .toSet()

        // Get named re-exports (export { FOO } from './module')
        val namedReexports = indexTsExportQuery
            .execute(rootNode, fileBody, currentPath)
            .map { (export, _) -> export.alias ?: export.identifier }
            .toSet()

        return declarations + namedReexports
    }
}
