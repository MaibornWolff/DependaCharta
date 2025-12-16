package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.tsconfig

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.DirectImport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import java.io.File

object PathAliasResolver {
    fun resolve(
        import: DirectImport,
        config: TsConfigData,
        tsconfigDir: File,
        analysisRoot: File
    ): Path? {
        val compilerOptions = config.compilerOptions ?: return null
        val importPath = import.directPath

        val matchedPath = findMatchingPath(importPath, compilerOptions.paths)
        val resolvedPath = if (matchedPath != null) {
            matchedPath
        } else if (compilerOptions.baseUrl != null) {
            "$importPath"
        } else {
            return null
        }

        val fullPath = if (compilerOptions.baseUrl != null) {
            val normalizedBaseUrl = normalizeBaseUrl(compilerOptions.baseUrl)
            "$normalizedBaseUrl/$resolvedPath"
        } else {
            resolvedPath
        }

        val absolutePath = tsconfigDir.resolve(fullPath).canonicalFile
        val relativePath = makeRelativeToAnalysisRoot(absolutePath, analysisRoot)

        return Path(relativePath.split("/").filter { it.isNotEmpty() })
    }

    private fun findMatchingPath(importPath: String, paths: Map<String, List<String>>?): String? {
        if (paths == null) {
            return null
        }

        for ((pattern, mappings) in paths) {
            if (pattern == importPath) {
                return mappings.firstOrNull()
            }

            if (pattern.endsWith("/*")) {
                val prefix = pattern.removeSuffix("/*")
                if (importPath.startsWith(prefix) && (importPath.length == prefix.length || importPath[prefix.length] == '/')) {
                    val suffix = if (importPath.length > prefix.length) importPath.substring(prefix.length + 1) else ""
                    val mapping = mappings.firstOrNull() ?: continue
                    if (mapping.endsWith("/*")) {
                        return mapping.removeSuffix("/*") + if (suffix.isNotEmpty()) "/$suffix" else ""
                    }
                }
            }
        }

        return null
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        return baseUrl.removePrefix("./").removeSuffix("/")
    }

    private fun makeRelativeToAnalysisRoot(absolutePath: File, analysisRoot: File): String {
        val canonicalAnalysisRoot = analysisRoot.canonicalFile
        val absolutePathStr = absolutePath.path
        val analysisRootStr = canonicalAnalysisRoot.path

        return if (absolutePathStr.startsWith(analysisRootStr)) {
            absolutePathStr.substring(analysisRootStr.length).removePrefix("/")
        } else {
            absolutePathStr.removePrefix("/")
        }
    }
}
