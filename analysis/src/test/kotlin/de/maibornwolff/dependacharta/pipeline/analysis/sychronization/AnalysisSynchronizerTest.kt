package de.maibornwolff.dependacharta.pipeline.analysis.sychronization

import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.build
import de.maibornwolff.dependacharta.pipeline.analysis.synchronization.AnalysisRecord
import de.maibornwolff.dependacharta.pipeline.analysis.synchronization.AnalysisSynchronizer
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.io.FileNotFoundException

class AnalysisSynchronizerTest {
    private val testee: AnalysisSynchronizer = AnalysisSynchronizer()

    @AfterEach
    fun cleanupTestFiles() {
        testee.deleteTempFiles()
    }

    @Test
    fun `Saves FileReport`() {
        // given
        val node = Node.build(Path(listOf("de", "maibornwolff")), "de/maibornwolff")
        val expectedFileReport = FileReport(listOf(node))

        // when
        val fileReportId = testee.saveFileReport(expectedFileReport)

        // then
        val fileReportJson = File("dependacharta_temp/records/$fileReportId.json").readText(Charsets.UTF_8)
        val fileReport = Json.decodeFromString<FileReport>(fileReportJson)
        assertThat(fileReport).isEqualTo(fileReport)
    }

    @Test
    fun `Loads FileReport`() {
        // given
        val node = Node.build(Path(listOf("de", "maibornwolff")), "de/maibornwolff")
        val expected = FileReport(listOf(node))
        val fileReportId = testee.saveFileReport(expected)

        // given
        val savedReport = testee.readFileReport(fileReportId)

        // then
        assertThat(savedReport).isEqualTo(expected)
    }

    @Test
    fun `Throws an exeception, if there is no FileReport with the given id`() {
        // given & when & then
        assertThrows<FileNotFoundException> { testee.readFileReport("non-existing-id") }
    }

    @Test
    fun `Throws an exeception, when passing null as id of FileReport`() {
        // given & when & then
        assertThrows<IllegalArgumentException> { testee.readFileReport(null) }
    }

    @Test
    fun `Saves an analysis record`() {
        // given
        val expectedAnalysisRecord = AnalysisRecord(mapOf("file" to null))

        // when
        testee.saveAnalysisRecord(expectedAnalysisRecord)

        // then
        val analysisRecordJson = File("dependacharta_temp/analysis_record.json").readText(Charsets.UTF_8)
        val analysisRecord = AnalysisRecord(Json.decodeFromString<Map<String, String?>>(analysisRecordJson))
        assertThat(analysisRecord).isEqualTo(expectedAnalysisRecord)
    }

    @Test
    fun `Overwrites old analysis records`() {
        // given
        val oldAnalysisRecord = AnalysisRecord(mapOf("file" to null))
        testee.saveAnalysisRecord(oldAnalysisRecord)
        val expectedAnalysisRecord = AnalysisRecord(mapOf("file" to "some-id"))

        // when
        testee.saveAnalysisRecord(expectedAnalysisRecord)

        // then
        val analysisRecordJson = File("dependacharta_temp/analysis_record.json").readText(Charsets.UTF_8)
        val analysisRecord = AnalysisRecord(Json.decodeFromString<Map<String, String?>>(analysisRecordJson))
        assertThat(analysisRecord).isEqualTo(expectedAnalysisRecord)
    }

    @Test
    fun `Loads an analysis record`() {
        // given
        val expectedAnalysisRecord = AnalysisRecord(mapOf("file" to null))
        testee.saveAnalysisRecord(expectedAnalysisRecord)

        // when
        val analysisRecord = testee.getAnalysisRecord()

        // then
        assertThat(analysisRecord).isEqualTo(expectedAnalysisRecord)
    }

    @Test
    fun `Throws an exception if analysis record does not exist`() {
        // given & when & then
        assertThrows<FileNotFoundException> { testee.getAnalysisRecord() }
    }

    @Test
    fun `Has an ongoing analysis if the temp directory exists`() {
        // given
        testee.saveAnalysisRecord(AnalysisRecord(mapOf("file" to null)))

        // when
        val isOngoing = testee.hasOngoingAnalysis()

        // then
        assertThat(isOngoing).isEqualTo(true)
    }

    @Test
    fun `Has no ongoing analysis if the temp directory does not exist`() {
        // given & when
        val isOngoing = testee.hasOngoingAnalysis()

        // then
        assertThat(isOngoing).isEqualTo(false)
    }

    @Test
    fun `Deletes temp directory`() {
        // given
        testee.saveAnalysisRecord(AnalysisRecord(mapOf("file" to null)))

        // when
        testee.deleteTempFiles()

        // then
        assertThrows<FileNotFoundException> { testee.getAnalysisRecord() }
    }
}