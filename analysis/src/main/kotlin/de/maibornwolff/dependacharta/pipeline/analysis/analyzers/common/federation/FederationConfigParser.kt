package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.federation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Parses package.json files to extract Module Federation configuration.
 */
object FederationConfigParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(packageJsonFile: File): FederationConfigData? {
        if (!packageJsonFile.exists()) {
            return null
        }

        return try {
            val content = packageJsonFile.readText()
            val packageJson = json.decodeFromString<PackageJsonData>(content)
            packageJson.federation?.toFederationConfigData()
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
private data class PackageJsonData(
    val name: String? = null,
    val federation: FederationField? = null
)

@Serializable
private data class FederationField(
    val name: String? = null,
    val remotes: Map<String, String>? = null,
    val exposes: Map<String, String>? = null
) {
    fun toFederationConfigData(): FederationConfigData {
        return FederationConfigData(
            name = name,
            remotes = remotes ?: emptyMap(),
            exposes = exposes ?: emptyMap()
        )
    }
}
