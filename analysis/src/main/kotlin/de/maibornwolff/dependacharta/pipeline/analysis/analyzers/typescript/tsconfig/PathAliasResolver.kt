package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.tsconfig

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.DirectImport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import java.io.File

object PathAliasResolver {
    // Main entry point: resolves "@app/models" with paths config to Path(["src", "app", "models"])
    fun resolve(
        import: DirectImport,
        config: TsConfigData,
        tsconfigDir: File,
        analysisRoot: File
    ): Path? {
        val compilerOptions = config.compilerOptions ?: return null
        val resolvedPath = resolveAgainstConfig(import.directPath, compilerOptions) ?: return null
        val absolutePath = computeAbsolutePath(resolvedPath, compilerOptions.baseUrl, tsconfigDir)
        return convertToRelativePath(absolutePath, analysisRoot)
    }

    // Resolves import path using path mappings or baseUrl fallback
    // Example: "@app/models" with "@app/*" -> "src/app/*" returns "src/app/models"
    private fun resolveAgainstConfig(
        importPath: String,
        options: CompilerOptions
    ): String? {
        val matchedPath = findMatchingPath(importPath, options.paths)
        if (matchedPath != null) {
            return matchedPath
        }
        if (options.baseUrl != null) {
            return importPath
        }
        return null
    }

    // Computes absolute path by combining baseUrl and resolved path
    // Example: "src/app/models" with baseUrl "." in "/project" -> "/project/src/app/models"
    private fun computeAbsolutePath(
        resolvedPath: String,
        baseUrl: String?,
        tsconfigDir: File
    ): File {
        val fullPath = if (baseUrl != null) {
            val normalizedBaseUrl = baseUrl.removePrefix("./").removeSuffix("/")
            "$normalizedBaseUrl/$resolvedPath"
        } else {
            resolvedPath
        }
        return tsconfigDir.resolve(fullPath).canonicalFile
    }

    // Converts absolute path to project-relative Path object
    // Example: "/project/src/app/models" relative to "/project" -> Path(["src", "app", "models"])
    private fun convertToRelativePath(
        absolutePath: File,
        analysisRoot: File
    ): Path {
        val relativePath = makeRelativeToAnalysisRoot(absolutePath, analysisRoot)
        return Path(relativePath.split("/").filter { it.isNotEmpty() })
    }

    // Finds first matching path mapping for import path
    // Example: "@app/models" with "@app/*" -> "src/app/*" returns "src/app/models"
    private fun findMatchingPath(
        importPath: String,
        paths: Map<String, List<String>>?
    ): String? {
        if (paths == null) {
            return null
        }

        for ((pattern, mappings) in paths) {
            val matched = tryMatchPattern(importPath, pattern, mappings)
            if (matched != null) {
                return matched
            }
        }

        return null
    }

    // Routes to exact match or wildcard match based on pattern
    // Example: "core" with pattern "core" -> exact match, "core/models" with "core/*" -> wildcard
    private fun tryMatchPattern(
        importPath: String,
        pattern: String,
        mappings: List<String>
    ): String? {
        if (pattern == importPath) {
            return mappings.firstOrNull()
        }

        if (pattern.endsWith("/*")) {
            return tryWildcardMatch(importPath, pattern, mappings)
        }

        return null
    }

    // Handles wildcard pattern matching: "@app/models" with "@app/*" -> "src/app/*" = "src/app/models"
    private fun tryWildcardMatch(
        importPath: String,
        pattern: String,
        mappings: List<String>
    ): String? {
        val prefix = pattern.removeSuffix("/*")

        if (!matchesPrefix(importPath, prefix)) {
            return null
        }

        val suffix = extractSuffix(importPath, prefix)
        val mapping = mappings.firstOrNull() ?: return null

        if (mapping.endsWith("/*")) {
            return substituteWildcard(mapping, suffix)
        }

        return null
    }

    // Validates prefix with boundary: "core/models" matches "core", but "coreutils" does not
    private fun matchesPrefix(
        importPath: String,
        prefix: String
    ): Boolean {
        if (!importPath.startsWith(prefix)) {
            return false
        }
        // Match if exact or followed by slash (word boundary)
        return importPath.length == prefix.length || importPath[prefix.length] == '/'
    }

    // Extracts remainder after prefix: "core/models/user" with "core" -> "models/user"
    private fun extractSuffix(
        importPath: String,
        prefix: String
    ): String {
        return if (importPath.length > prefix.length) {
            importPath.substring(prefix.length + 1)
        } else {
            ""
        }
    }

    // Substitutes wildcard: "src/app/*" with "models/user" -> "src/app/models/user"
    private fun substituteWildcard(
        mapping: String,
        suffix: String
    ): String {
        val baseMapping = mapping.removeSuffix("/*")
        return if (suffix.isNotEmpty()) {
            "$baseMapping/$suffix"
        } else {
            baseMapping
        }
    }

    private fun makeRelativeToAnalysisRoot(
        absolutePath: File,
        analysisRoot: File
    ): String {
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
