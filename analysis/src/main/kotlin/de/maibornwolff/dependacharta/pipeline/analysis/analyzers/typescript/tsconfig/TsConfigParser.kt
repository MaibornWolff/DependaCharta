package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.tsconfig

import kotlinx.serialization.json.Json
import java.io.File

object TsConfigParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(tsconfigFile: File): TsConfigData? {
        if (!tsconfigFile.exists()) {
            return null
        }

        return try {
            val content = tsconfigFile.readText()
            json.decodeFromString<TsConfigData>(content)
        } catch (e: Exception) {
            null
        }
    }
}
