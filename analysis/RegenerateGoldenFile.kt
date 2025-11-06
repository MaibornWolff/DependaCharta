import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.processing.ProcessingPipeline
import kotlinx.serialization.json.Json
import java.io.File

fun main() {
    println("Regenerating golden file...")
    
    val fileReportFiles = File("src/test/resources/pipeline/projectreport/filereports")
        .walk()
        .filter { it.isFile && it.name.endsWith(".json") }
        .toSet()

    val fileReportJsons = fileReportFiles.map { it.readText(Charsets.UTF_8) }
    val fileReports = fileReportJsons.map { Json.decodeFromString<FileReport>(it) }

    ProcessingPipeline.run("java-example", "src/test/resources/pipeline/projectreport", fileReports, false)
    
    println("Golden file regenerated at: src/test/resources/pipeline/projectreport/java-example.cg.json")
}