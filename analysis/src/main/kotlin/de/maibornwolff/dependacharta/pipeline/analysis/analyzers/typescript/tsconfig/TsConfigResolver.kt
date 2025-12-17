package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.tsconfig

import java.io.File

class TsConfigResolver {
    private val cache = mutableMapOf<String, TsConfigData?>()

    companion object {
        private const val TSCONFIG_FILENAME = "tsconfig.json"
    }

    fun findTsConfig(sourceFile: File): TsConfigResult? {
        val tsconfigFile = findTsConfigFile(sourceFile) ?: return null
        val data = resolveWithInheritance(tsconfigFile) ?: return null
        return TsConfigResult(data, tsconfigFile)
    }

    private fun findTsConfigFile(sourceFile: File): File? {
        var currentDir = if (sourceFile.isDirectory) sourceFile else sourceFile.parentFile

        while (currentDir != null) {
            val tsconfigFile = currentDir.resolve(TSCONFIG_FILENAME)
            if (tsconfigFile.exists()) {
                return tsconfigFile
            }
            currentDir = currentDir.parentFile
        }

        return null
    }

    private fun getCachedOrParse(tsconfigFile: File): TsConfigData? {
        val absolutePath = tsconfigFile.absolutePath
        return cache.getOrPut(absolutePath) {
            TsConfigParser.parse(tsconfigFile)
        }
    }

    private fun resolveWithInheritance(tsconfigFile: File): TsConfigData? {
        val config = getCachedOrParse(tsconfigFile) ?: return null

        if (config.extends == null) {
            return config
        }

        val parentFile = resolveExtendsPath(tsconfigFile, config.extends)
        if (!parentFile.exists()) {
            return config
        }

        val parentConfig = resolveWithInheritance(parentFile) ?: return config

        return mergeConfigs(parentConfig, config)
    }

    private fun resolveExtendsPath(
        tsconfigFile: File,
        extendsPath: String
    ): File {
        return if (File(extendsPath).isAbsolute) {
            File(extendsPath)
        } else {
            tsconfigFile.parentFile.resolve(extendsPath)
        }
    }

    private fun mergeConfigs(
        parent: TsConfigData,
        child: TsConfigData
    ): TsConfigData {
        val parentOptions = parent.compilerOptions
        val childOptions = child.compilerOptions

        if (parentOptions == null) {
            return child
        }

        if (childOptions == null) {
            return TsConfigData(
                compilerOptions = parentOptions,
                extends = null
            )
        }

        val mergedPaths = mutableMapOf<String, List<String>>()
        parentOptions.paths?.let { mergedPaths.putAll(it) }
        childOptions.paths?.let { mergedPaths.putAll(it) }

        val mergedCompilerOptions = CompilerOptions(
            baseUrl = childOptions.baseUrl ?: parentOptions.baseUrl,
            paths = if (mergedPaths.isEmpty()) null else mergedPaths
        )

        return TsConfigData(
            compilerOptions = mergedCompilerOptions,
            extends = null
        )
    }
}
