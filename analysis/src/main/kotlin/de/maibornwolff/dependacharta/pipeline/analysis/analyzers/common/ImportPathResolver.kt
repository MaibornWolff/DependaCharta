package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.bundler.BundlerAliasResolver
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.bundler.BundlerConfigResolver
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.federation.FederationAliasResolver
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.federation.FederationConfigResolver
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.DirectImport
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.Import
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.toImport
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.resolveRelativePath
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.stripSourceFileExtension
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.tsconfig.PathAliasResolver
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.tsconfig.TsConfigResolver
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path

/**
 * Resolves import paths using a fallback chain:
 * 1. tsconfig.json / jsconfig.json path aliases
 * 2. Bundler config aliases (webpack, vite, vue.config)
 * 3. Module Federation remotes
 * 4. Relative path resolution
 *
 * Note: No dedicated tests - this class orchestrates individually-tested resolvers.
 * Integration coverage provided by JavascriptEs6ImportsQuery and TypescriptImportStatementQuery tests.
 */
class ImportPathResolver {
    private val tsConfigResolver = TsConfigResolver()
    private val bundlerConfigResolver = BundlerConfigResolver()
    private val federationConfigResolver = FederationConfigResolver()

    /**
     * Resolves an import path string to an absolute Path.
     *
     * @param importString The import path as it appears in the source code
     * @param currentFilePath The path of the file containing the import
     * @param fileInfo Optional file info for alias resolution (requires analysisRoot)
     * @param stripExtension Whether to strip file extensions before resolution
     * @return The resolved Path
     */
    fun resolve(
        importString: String,
        currentFilePath: Path,
        fileInfo: FileInfo? = null,
        stripExtension: Boolean = true
    ): Path {
        val processedImportString = if (stripExtension) {
            importString.stripSourceFileExtension()
        } else {
            importString
        }
        val import = processedImportString.toImport()

        return resolveImport(import, currentFilePath, fileInfo)
    }

    private fun resolveImport(
        import: Import,
        currentFilePath: Path,
        fileInfo: FileInfo?
    ): Path {
        if (import is DirectImport && fileInfo?.analysisRoot != null) {
            val resolved = tryResolveDirectImport(import, fileInfo)
            if (resolved != null) {
                return resolved
            }
        }

        return resolveRelativePath(import, currentFilePath)
    }

    private fun tryResolveDirectImport(
        import: DirectImport,
        fileInfo: FileInfo
    ): Path? {
        val sourceFile = fileInfo.analysisRoot!!.resolve(fileInfo.physicalPath)

        // Try tsconfig/jsconfig paths first
        val tsConfigResolved = tryResolveTsConfig(import, sourceFile, fileInfo)
        if (tsConfigResolved != null) {
            return tsConfigResolved
        }

        // Fall back to bundler config aliases (webpack, vite, vue.config)
        val bundlerResolved = tryResolveBundlerConfig(import, sourceFile, fileInfo)
        if (bundlerResolved != null) {
            return bundlerResolved
        }

        // Fall back to Module Federation remotes
        val federationResolved = tryResolveFederation(import, sourceFile, fileInfo)
        if (federationResolved != null) {
            return federationResolved
        }

        return null
    }

    private fun tryResolveTsConfig(
        import: DirectImport,
        sourceFile: java.io.File,
        fileInfo: FileInfo
    ): Path? {
        val tsConfigResult = tsConfigResolver.findTsConfig(sourceFile) ?: return null
        return PathAliasResolver.resolve(
            import,
            tsConfigResult.data,
            tsConfigResult.file.parentFile,
            fileInfo.analysisRoot!!
        )
    }

    private fun tryResolveBundlerConfig(
        import: DirectImport,
        sourceFile: java.io.File,
        fileInfo: FileInfo
    ): Path? {
        val bundlerResult = bundlerConfigResolver.findBundlerConfig(sourceFile) ?: return null
        return BundlerAliasResolver.resolve(
            import,
            bundlerResult.data,
            fileInfo.analysisRoot!!
        )
    }

    private fun tryResolveFederation(
        import: DirectImport,
        sourceFile: java.io.File,
        fileInfo: FileInfo
    ): Path? {
        val federationResult = federationConfigResolver.findConsumerConfig(sourceFile) ?: return null
        return FederationAliasResolver.resolve(
            import,
            federationResult,
            federationConfigResolver,
            fileInfo.analysisRoot!!
        )
    }
}
