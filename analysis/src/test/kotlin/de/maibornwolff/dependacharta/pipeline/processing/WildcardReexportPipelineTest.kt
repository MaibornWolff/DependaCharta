package de.maibornwolff.dependacharta.pipeline.processing

import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.processing.model.ProjectReportDto
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File

class WildcardReexportPipelineTest {
    private val outputFileName = "wildcard-reexport-test"
    private val outputDirectoryName = "testresult"

    @AfterEach
    fun tearDown() {
        val projectReportFile = File("$outputDirectoryName/$outputFileName.cg.json")
        projectReportFile.delete()
        File(outputDirectoryName).delete()
    }

    @Test
    fun `REEXPORT nodes should depend on source not themselves`() {
        // given - Simulate wildcard re-export scenario:
        // common/constants/index.ts exports FOO
        // common/index.ts does: export * from './constants'
        val sourceFoo = Node(
            pathWithName = Path(listOf("common", "constants", "index", "FOO")),
            physicalPath = "common/constants/index.ts",
            language = SupportedLanguage.TYPESCRIPT,
            nodeType = NodeType.VARIABLE,
            dependencies = setOf(Dependency(Path(listOf("common", "constants", "index")), isWildcard = true)),
            usedTypes = emptySet()
        )

        val reexportFoo = Node(
            pathWithName = Path(listOf("common", "index", "FOO")),
            physicalPath = "common/index.ts",
            language = SupportedLanguage.TYPESCRIPT,
            nodeType = NodeType.REEXPORT,
            dependencies = setOf(
                Dependency(Path(listOf("common", "constants", "index", "FOO")), isWildcard = false),
                Dependency(Path(listOf("common", "index")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("FOO"))
        )

        val fileReports = listOf(
            FileReport(nodes = listOf(sourceFoo)),
            FileReport(nodes = listOf(reexportFoo))
        )

        // when
        ProcessingPipeline.run(outputFileName, outputDirectoryName, fileReports, false)

        // then
        val projectReportFile = File("$outputDirectoryName/$outputFileName.cg.json")
        val projectReportJson = projectReportFile.readText(Charsets.UTF_8)
        val projectReport = Json { explicitNulls = false }.decodeFromString<ProjectReportDto>(projectReportJson)

        val reexportLeaf = projectReport.leaves["common.index.FOO"]!!
        val dependencyPaths = reexportLeaf.dependencies.keys

        assertThat(dependencyPaths)
            .withFailMessage(
                "REEXPORT node should depend on source (common.constants.index.FOO), not itself (common.index.FOO). " +
                    "Found: $dependencyPaths"
            ).contains("common.constants.index.FOO")
    }
}
