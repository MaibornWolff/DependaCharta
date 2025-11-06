package de.maibornwolff.dependacharta.pipeline.processing.reporting

import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeDependencies
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.build
import de.maibornwolff.dependacharta.pipeline.processing.model.ProjectReportDto
import de.maibornwolff.dependacharta.pipeline.processing.levelization.model.GraphNodeBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReportServiceTest {
    @Test
    fun `should create a project report`() {
        // given
        val node = Node.build(
            Path(listOf("de", "maibornwolff", "main")),
            resolvedNodeDependencies = NodeDependencies(
                setOf(Dependency(Path(listOf("de", "maibornwolff", "helper")))),
                emptySet()
            )
        )
        val resolvedNodes = listOf(node)
        val cyclicEdges = mapOf("de.maibornwolff.main" to setOf("de.maibornwolff.helper"))
        val graphNode = GraphNodeBuilder(
            id = "de.maibornwolff.main",
            dependencies = setOf("de.maibornwolff.helper")
        ).build()

        // when
        val report = ReportService.createProjectReport(
            resolvedNodes,
            cyclicEdges,
            listOf(graphNode)
        )

        // then
        val allNodesMap = mapOf(graphNode.id to graphNode)
        val levelsByNodeId = mapOf(graphNode.id to (graphNode.level ?: 0))
        val expected = ProjectReportDto(
            setOf(graphNode.toProjectNodeDto(cyclicEdges, allNodesMap)),
            mapOf("de.maibornwolff.main" to resolvedNodes[0].toLeafInformationDto(cyclicEdges, levelsByNodeId))
        )
        assertThat(report).isEqualTo(expected)
    }
}