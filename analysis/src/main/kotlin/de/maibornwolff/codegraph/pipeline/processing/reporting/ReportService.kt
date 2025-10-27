package de.maibornwolff.codegraph.pipeline.processing.reporting

import de.maibornwolff.codegraph.pipeline.analysis.model.Node
import de.maibornwolff.codegraph.pipeline.processing.levelization.model.GraphNode
import de.maibornwolff.codegraph.pipeline.processing.model.ProjectReportDto

class ReportService {
    companion object {
        fun createProjectReport(
            resolvedNodes: Collection<Node>,
            cyclicEdgesByLeaf: Map<String, Set<String>>,
            nodes: List<GraphNode>
        ) = ProjectReportDto(
            projectTreeRoots = nodes.map { it.toProjectNodeDto(cyclicEdgesByLeaf) }.toSet(),
            leaves = resolvedNodes.associateBy(
                { it.pathWithName.withDots() },
                { node -> node.toLeafInformationDto(cyclicEdgesByLeaf) }
            )
        )
    }
}
