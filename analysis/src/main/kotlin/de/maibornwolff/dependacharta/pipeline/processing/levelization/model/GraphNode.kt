package de.maibornwolff.dependacharta.pipeline.processing.levelization.model

import de.maibornwolff.dependacharta.pipeline.analysis.model.TypeOfUsage
import de.maibornwolff.dependacharta.pipeline.processing.model.EdgeInfoDto
import de.maibornwolff.dependacharta.pipeline.processing.model.ProjectNodeDto

data class GraphNode(
    val id: String,
    val parent: String?,
    val children: List<GraphNode>,
    val level: Int? = null,
    val dependencies: Set<String> = emptySet(),
    val edges: Set<GraphEdge> = emptySet()
) {
    companion object

    fun toProjectNodeDto(
        cyclicEdgesByLeaf: Map<String, Set<String>>,
        allNodesMap: Map<String, GraphNode>
    ): ProjectNodeDto {
        val isLeaf = children.isEmpty()
        val childDtos = children.map { it.toProjectNodeDto(cyclicEdgesByLeaf, allNodesMap) }.toSet()

        val internalDependencies = if (isLeaf) {
            dependencies.associateWith { dependency ->
                val edgeTypes = edges.filter { it.target == dependency }.map { it.type }.toSet()
                val edgeTypesJoined = edgeTypes.joinToString(",")
                dependency.toEdgeInfoDto(
                    cyclicEdges = cyclicEdgesByLeaf[id],
                    type = if (edgeTypes.isEmpty()) TypeOfUsage.USAGE.rawValue else edgeTypesJoined,
                    sourceLevel = level ?: 0,
                    targetLevel = allNodesMap[dependency]?.level ?: 0
                )
            }
        } else {
            // For parent nodes, we need to find which child nodes have dependencies to the target
            // and calculate isPointingUpwards based on the relationship between THIS node and the target node
            childDtos
                .flatMap { it.containedInternalDependencies.entries }
                .groupBy({ it.key }, { it.value })
                .mapValues { (targetId, edgeInfos) ->
                    // Find the target node to get its level
                    val targetNode = allNodesMap[targetId]
                    // Calculate isPointingUpwards based on THIS node's level vs target's level
                    edgeInfos.sum(
                        sourceLevel = level ?: 0,
                        targetLevel = targetNode?.level ?: 0
                    )
                }
        }

        return ProjectNodeDto(
            leafId = if (isLeaf) id else null,
            name = id.split(".").last(),
            children = childDtos,
            level = level ?: -1,
            containedLeaves = if (isLeaf) setOf(id) else childDtos.flatMap { it.containedLeaves }.toSet(),
            containedInternalDependencies = internalDependencies
        )
    }

    private fun String.toEdgeInfoDto(
        cyclicEdges: Set<String>?,
        type: String,
        sourceLevel: Int,
        targetLevel: Int
    ) = EdgeInfoDto(
        isCyclic = cyclicEdges?.contains(this) ?: false,
        weight = 1,
        type = type,
        isPointingUpwards = sourceLevel <= targetLevel
    )

    private fun List<EdgeInfoDto>.sum(
        sourceLevel: Int,
        targetLevel: Int
    ) = EdgeInfoDto(
        isCyclic = any { it.isCyclic },
        weight = sumOf { it.weight },
        type = map { it.type }.toSet().joinToString(","),
        isPointingUpwards = sourceLevel <= targetLevel
    )
}
