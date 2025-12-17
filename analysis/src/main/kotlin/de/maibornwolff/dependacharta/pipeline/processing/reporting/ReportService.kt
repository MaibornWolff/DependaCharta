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
            // Create a virtual root that wraps all top-level nodes to ensure
            // cross-root dependencies can find a common ancestor
            val (nodesWithParent, unifiedRoot) = if (nodes.size > 1) {
                val virtualRootId = "__virtual_root__"
                val updatedNodes = nodes.map { it.copy(parent = virtualRootId) }
                val virtualRoot = GraphNode(
                    id = virtualRootId,
                    parent = null,
                    children = updatedNodes,
                    level = null,
                    dependencies = emptySet(),
                    edges = emptySet()
                )
                Pair(updatedNodes, virtualRoot)
            } else {
                Pair(nodes, nodes.first())
            }

            return ProjectReportDto(
                projectTreeRoots = nodesWithParent.map { it.toProjectNodeDto(cyclicEdgesByLeaf, root = unifiedRoot) }.toSet(),
                leaves = resolvedNodes.associateBy(
                    { it.pathWithName.withDots() },
                    { node -> node.toLeafInformationDto(cyclicEdgesByLeaf) }
                )
            )
        }
    }
}
