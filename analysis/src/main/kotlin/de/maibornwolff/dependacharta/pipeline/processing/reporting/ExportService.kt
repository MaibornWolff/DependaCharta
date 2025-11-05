package de.maibornwolff.dependacharta.pipeline.processing.reporting

import de.maibornwolff.dependacharta.pipeline.processing.model.ProjectReportDto
import kotlinx.serialization.json.Json

class ExportService {
    companion object {
        private val compactJson = Json {
            explicitNulls = false
            encodeDefaults = true
        }

        fun toJson(projectReport: ProjectReportDto) = compactJson.encodeToString(value = projectReport)
    }
}
