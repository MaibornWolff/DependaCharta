package de.maibornwolff.codegraph.pipeline.processing.reporting

import de.maibornwolff.codegraph.pipeline.processing.model.ProjectReportDto
import kotlinx.serialization.json.Json

class ExportService {
    companion object {
        private val compactJson = Json { explicitNulls = false }

        fun toJson(projectReport: ProjectReportDto) = compactJson.encodeToString(value = projectReport)
    }
}
