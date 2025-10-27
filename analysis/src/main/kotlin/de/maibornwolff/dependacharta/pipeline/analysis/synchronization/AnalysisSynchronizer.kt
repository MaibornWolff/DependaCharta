package de.maibornwolff.codegraph.pipeline.analysis.synchronization

import de.maibornwolff.codegraph.pipeline.analysis.model.FileReport
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.io.path.createDirectories

class AnalysisSynchronizer {
    private val tempDir = "codegraph_temp/"
    private val recordFileNameName = "analysis_record.json"
    private val recordDir = "${tempDir}records/"

    private val recordLock = Any()
    private val directoryLock = Any()

    fun hasOngoingAnalysis(): Boolean = File(tempDir + recordFileNameName).exists()

    fun getAnalysisRecord(): AnalysisRecord {
        synchronized(recordLock) {
            val recordFile = File(tempDir + recordFileNameName)
            if (!recordFile.exists()) {
                throw FileNotFoundException(recordFile.absolutePath)
            }
            val recordJson = recordFile.readText(Charsets.UTF_8)
            return AnalysisRecord(Json.decodeFromString<Map<String, String?>>(recordJson))
        }
    }

    fun saveAnalysisRecord(record: AnalysisRecord) {
        synchronized(recordLock) {
            val recordFilePath = File(tempDir)
            recordFilePath.toPath().createDirectories()
            val recordFile = File(tempDir + recordFileNameName)
            if (recordFile.exists()) {
                recordFile.delete()
            }
            recordFile.printWriter(Charsets.UTF_8).use {
                it.write(Json.encodeToString(record.pathToFileReport))
            }
        }
    }

    fun saveFileReport(fileReport: FileReport): String {
        val id = UUID.randomUUID().toString()
        val fileName = "$id.json"

        synchronized(directoryLock) {
            val reportFilePath = File(recordDir)
            reportFilePath.toPath().createDirectories()
        }

        File("$recordDir$fileName").printWriter(Charsets.UTF_8).use {
            it.write(Json.encodeToString(fileReport))
        }
        return id
    }

    fun saveFileReport(
        fileReport: FileReport,
        className: String
    ): String {
        val id = UUID.randomUUID().toString()
        val fileName = "$className.json"

        synchronized(directoryLock) {
            val reportFilePath = File(recordDir)
            reportFilePath.toPath().createDirectories()
        }

        File("$recordDir$fileName").printWriter(Charsets.UTF_8).use {
            it.write(Json.encodeToString(fileReport))
        }
        return id
    }

    fun readFileReport(id: String?): FileReport {
        requireNotNull(id) { "id must not be null" }
        val reportFile = File("$recordDir$id.json")
        if (!reportFile.exists()) {
            throw FileNotFoundException(reportFile.absolutePath)
        }
        val recordJson = reportFile.readText(Charsets.UTF_8)
        return Json.decodeFromString<FileReport>(recordJson)
    }

    fun deleteTempFiles() = File(tempDir).deleteRecursively()
}

data class AnalysisRecord(
    val pathToFileReport: Map<String, String?>
)