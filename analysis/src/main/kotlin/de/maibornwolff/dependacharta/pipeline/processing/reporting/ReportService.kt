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
            val levelsByNodeId = buildLevelMap(nodes)
            val allNodesMap = buildNodeMap(nodes)
            return ProjectReportDto(
                projectTreeRoots = nodes.map { it.toProjectNodeDto(cyclicEdgesByLeaf, allNodesMap) }.toSet(),
                leaves = resolvedNodes.associateBy(
                    { it.pathWithName.withDots() },
                    { node -> node.toLeafInformationDto(cyclicEdgesByLeaf, levelsByNodeId) }
                )
            )
        }

        private fun buildLevelMap(nodes: List<GraphNode>): Map<String, Int> {
            val levelMap = mutableMapOf<String, Int>()

            fun collectLevels(node: GraphNode) {
                if (node.level != null) {
                    levelMap[node.id] = node.level
                }
                node.children.forEach { collectLevels(it) }
            }

            nodes.forEach { collectLevels(it) }
            return levelMap
        }

        private fun buildNodeMap(nodes: List<GraphNode>): Map<String, GraphNode> {
            val nodeMap = mutableMapOf<String, GraphNode>()

            fun collectNodes(node: GraphNode) {
                nodeMap[node.id] = node
                node.children.forEach { collectNodes(it) }
            }

            nodes.forEach { collectNodes(it) }
            return nodeMap
        }
    }
}
