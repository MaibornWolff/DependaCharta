package de.maibornwolff.codegraph.pipeline.processing.reporting

import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency
import de.maibornwolff.codegraph.pipeline.analysis.model.Node
import de.maibornwolff.codegraph.pipeline.analysis.model.NodeDependencies
import de.maibornwolff.codegraph.pipeline.analysis.model.Path
import de.maibornwolff.codegraph.pipeline.analysis.model.build
import de.maibornwolff.codegraph.pipeline.processing.model.ProjectReportDto
import de.maibornwolff.codegraph.pipeline.processing.levelization.model.GraphNodeBuilder
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
        val expected = ProjectReportDto(
            setOf(graphNode.toProjectNodeDto(cyclicEdges)),
            mapOf("de.maibornwolff.main" to resolvedNodes[0].toLeafInformationDto(cyclicEdges))
        )
        assertThat(report).isEqualTo(expected)
    }
}