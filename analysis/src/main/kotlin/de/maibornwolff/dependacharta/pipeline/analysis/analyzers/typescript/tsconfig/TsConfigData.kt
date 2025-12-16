package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.tsconfig

import kotlinx.serialization.Serializable

@Serializable
data class TsConfigData(
    val compilerOptions: CompilerOptions? = null,
    val extends: String? = null
) {
    companion object {
        val EMPTY = TsConfigData()
    }
}

@Serializable
data class CompilerOptions(
    val baseUrl: String? = null,
    val paths: Map<String, List<String>>? = null
)
