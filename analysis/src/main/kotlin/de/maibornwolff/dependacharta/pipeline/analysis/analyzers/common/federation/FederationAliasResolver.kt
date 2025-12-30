package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.federation

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.DirectImport
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.stripSourceFileExtension
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import java.io.File

/**
 * Resolves Module Federation imports to actual file paths.
 *
 * Example:
 * - Import: "Shared/Utils"
 * - Consumer remotes: { "Shared": "shared@/app/shared/remoteEntry.js" }
 * - Producer exposes: { "./Utils": "./src/utils.js" }
 * - Result: Path to modules/shared/src/utils.js
 */
object FederationAliasResolver {
    fun resolve(
        import: DirectImport,
        consumerConfig: FederationConfigResult,
        resolver: FederationConfigResolver,
        analysisRoot: File
    ): Path? {
        val importPath = import.directPath

        // Parse import: "Shared/Utils" -> remoteName="Shared", subPath="Utils"
        val (remoteName, subPath) = parseImportPath(importPath) ?: return null

        // Look up remote in consumer's federation.remotes
        val remoteUrl = consumerConfig.data.remotes[remoteName] ?: return null

        // Extract module name from remote URL: "shared@/app/..." -> "shared"
        val producerModuleName = extractModuleName(remoteUrl) ?: return null

        // Find producer module in monorepo
        val producerConfig = resolver.findProducerModule(
            consumerConfig.moduleDir,
            producerModuleName
        ) ?: return null

        // Look up exposed path: "./Utils" -> "./src/utils.js"
        val exposedKey = "./$subPath"
        val exposedPath = producerConfig.data.exposes[exposedKey] ?: return null

        // Resolve to actual file path
        val resolvedFile = producerConfig.moduleDir.resolve(exposedPath).canonicalFile

        return convertToRelativePath(resolvedFile, analysisRoot)
    }

    private fun parseImportPath(importPath: String): Pair<String, String>? {
        val slashIndex = importPath.indexOf('/')
        if (slashIndex <= 0) {
            return null
        }

        val remoteName = importPath.substring(0, slashIndex)
        val subPath = importPath.substring(slashIndex + 1)

        return remoteName to subPath
    }

    private fun extractModuleName(remoteUrl: String): String? {
        // Format: "moduleName@url" or just "moduleName"
        val atIndex = remoteUrl.indexOf('@')
        return if (atIndex > 0) {
            remoteUrl.substring(0, atIndex)
        } else {
            remoteUrl.ifEmpty { null }
        }
    }

    private fun convertToRelativePath(
        absolutePath: File,
        analysisRoot: File
    ): Path {
        val relativePath = makeRelativeToAnalysisRoot(absolutePath, analysisRoot.canonicalFile)
            .stripSourceFileExtension()
        return Path(relativePath.split("/").filter { it.isNotEmpty() })
    }

    private fun makeRelativeToAnalysisRoot(
        absolutePath: File,
        analysisRoot: File
    ): String {
        val path = absolutePath.path
        val root = analysisRoot.path

        return if (path.startsWith(root)) {
            path.substring(root.length).removePrefix("/")
        } else {
            path.removePrefix("/")
        }
    }
}
