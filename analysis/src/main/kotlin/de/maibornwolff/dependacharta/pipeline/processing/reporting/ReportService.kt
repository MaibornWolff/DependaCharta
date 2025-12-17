package de.maibornwolff.dependacharta.pipeline.processing.reporting

import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.processing.levelization.model.GraphNode
import de.maibornwolff.dependacharta.pipeline.processing.model.ProjectReportDto

class ReportService {
    companion object {
        fun createProjectReport(
            resolvedNodes: Collection<Node>,
            cyclicEdgesByLeaf: Map<String, Set<String>>,
            nodes: List<GraphNode>
        ): ProjectReportDto {
            val (nodesWithParent, unifiedRoot) = GraphNode.wrapInVirtualRootIfNeeded(nodes)

            return ProjectReportDto(
                projectTreeRoots = nodesWithParent.map { it.toProjectNodeDto(cyclicEdgesByLeaf, unifiedRoot) }.toSet(),
                leaves = resolvedNodes.associateBy(
                    { it.pathWithName.withDots() },
                    { node -> node.toLeafInformationDto(cyclicEdgesByLeaf) }
                )
            )
        }
    }
}
