package de.maibornwolff.codegraph.pipeline

import de.maibornwolff.codegraph.pipeline.shared.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Version(
    val version: String = "undefined"
) {
    override fun toString(): String = "Version $version"
}

class VersionProvider {
    fun get(): Version {
        val versionAsJsonString = getVersionFileContent()
        if (versionAsJsonString == null) {
            Logger.w("Could not read version file. Returning default version.")
            return Version()
        }

        return readVersionFromJson(versionAsJsonString)
    }

    private fun readVersionFromJson(jsonString: String): Version {
        try {
            val versionFromJson = Json.decodeFromString<Version>(jsonString)
            return if (versionFromJson.version == "") Version() else versionFromJson
        } catch (e: Exception) {
            Logger.e("Could not read version file. Cause: ${e.message}, Stacktrace: ${e.stackTraceToString()}")
            return Version()
        }
    }

    private fun getVersionFileContent(): String? {
        try {
            val inputStream = {}.javaClass.classLoader.getResourceAsStream("pipeline/version.json")
            return inputStream?.bufferedReader().use { it?.readText() }
        } catch (e: Exception) {
            Logger.e("Could not find version file. Cause: ${e.message}, Stacktrace: ${e.stackTraceToString()}")
            return null
        }
    }
}
