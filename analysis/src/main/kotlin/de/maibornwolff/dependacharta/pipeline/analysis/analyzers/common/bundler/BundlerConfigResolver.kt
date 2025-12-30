package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.bundler

import java.io.File

/**
 * Finds bundler config files (webpack, vite, vue) by walking up from a source file.
 * Caches parsed configs for performance.
 */
class BundlerConfigResolver {
    private val cache = mutableMapOf<String, BundlerConfigData?>()

    companion object {
        // Priority order: Vite (modern) > Vue CLI > Webpack (legacy)
        private val CONFIG_FILENAMES = listOf(
            "vite.config.ts",
            "vite.config.js",
            "vue.config.js",
            "webpack.config.js"
        )
    }

    fun findBundlerConfig(sourceFile: File): BundlerConfigResult? {
        val configFile = findConfigFile(sourceFile) ?: return null
        val data = getCachedOrParse(configFile) ?: return null
        return BundlerConfigResult(data, configFile)
    }

    private fun findConfigFile(sourceFile: File): File? {
        var currentDir = if (sourceFile.isDirectory) sourceFile else sourceFile.parentFile

        while (currentDir != null) {
            for (filename in CONFIG_FILENAMES) {
                val configFile = currentDir.resolve(filename)
                if (configFile.exists()) {
                    return configFile
                }
            }
            currentDir = currentDir.parentFile
        }

        return null
    }

    private fun getCachedOrParse(configFile: File): BundlerConfigData? {
        val absolutePath = configFile.absolutePath
        return cache.getOrPut(absolutePath) {
            BundlerConfigParser.parse(configFile)
        }
    }
}
