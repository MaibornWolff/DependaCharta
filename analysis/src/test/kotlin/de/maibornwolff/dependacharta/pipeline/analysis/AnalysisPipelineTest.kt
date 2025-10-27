package de.maibornwolff.dependacharta.pipeline.analysis

import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createDirectories

class AnalysisPipelineTest {
    @AfterEach
    fun cleanUp() {
        AnalysisPipeline.cleanTempFiles()
    }

    @Test
    fun `Analyses Java Project correctly`() {
        // given & when
        val fileReports = AnalysisPipeline.run(
            "src/test/resources/pipeline/persistence",
            false,
            listOf(SupportedLanguage.JAVA, SupportedLanguage.C_SHARP)
        )

        // then
        val expectedFileReportFiles = File("src/test/resources/pipeline/filereports")
            .walk()
            .filter { it.isFile && it.name.endsWith(".json") }
            .toSet()

        val fileReportJsons = expectedFileReportFiles.map { it.readText(Charsets.UTF_8) }
        val expectedFileReports = fileReportJsons.map { Json.decodeFromString<FileReport>(it) }
        val fileReportsWithoutPhysicalPath = fileReports.removePhysicalPath()
        val expectedFileReportsWithoutPhysicalPath = expectedFileReports.removePhysicalPath()
        assertThat(fileReportsWithoutPhysicalPath).containsExactlyInAnyOrderElementsOf(expectedFileReportsWithoutPhysicalPath)
    }

    @Test
    fun `should clean temp files`() {
        // given
        val dummyFile = "dependacharta_temp/i_should_not_be_here"
        File(dummyFile).mkdir()

        // when
        AnalysisPipeline.run(
            "src/test/resources/pipeline/persistence",
            true,
            listOf(SupportedLanguage.JAVA, SupportedLanguage.C_SHARP)
        )

        // then
        assertThat(File(dummyFile).exists()).isFalse
    }

    @Test
    fun `should not clean temp files`() {
        // given
        val dummyFilePath = "dependacharta_temp/i_should_be_here"
        val dummyFile = File(dummyFilePath)
        dummyFile.toPath().createDirectories()

        // when
        AnalysisPipeline.run(
            "src/test/resources/pipeline/persistence",
            false,
            listOf(SupportedLanguage.JAVA, SupportedLanguage.C_SHARP)
        )

        // then
        assertThat(File(dummyFilePath).exists()).isTrue
        dummyFile.delete()
    }

    @Test
    fun `should not create FileReports if no supported languages are provided`() {
        // when & given
        val fileReports = AnalysisPipeline.run(
            "src/test/resources/pipeline/persistence",
            true,
            emptyList()
        )

        // then
        assertThat(fileReports).isEmpty()
    }

    @Test
    fun `should save FileReports in temp directory`() {
        // given
        AnalysisPipeline.cleanTempFiles()

        // when
        AnalysisPipeline.run(
            "src/test/resources/pipeline/persistence",
            true,
            listOf(SupportedLanguage.JAVA, SupportedLanguage.C_SHARP)
        )

        // then
        assertThat(File("dependacharta_temp").exists()).isTrue
        assertThat(File("dependacharta_temp/records").listFiles()).hasSize(3)
    }

    private fun List<FileReport>.removePhysicalPath() =
        this.map { fileReport ->
            fileReport.copy(nodes = fileReport.nodes.map { node -> node.copy(physicalPath = "") })
        }
}
