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
        allNodes: Map<String, GraphNode> = emptyMap()
    ): ProjectNodeDto {
        val isLeaf = children.isEmpty()
        val childDtos = children.map { it.toProjectNodeDto(cyclicEdgesByLeaf, allNodes) }.toSet()

        val internalDependencies = if (isLeaf) {
            dependencies.associateWith { dependency ->
                val edgeTypes = edges.filter { it.target == dependency }.map { it.type }.toSet()
                val edgeTypesJoined = edgeTypes.joinToString(",")
                dependency.toEdgeInfoDto(
                    cyclicEdgesByLeaf[id],
                    if (edgeTypes.isEmpty()) TypeOfUsage.USAGE.rawValue else edgeTypesJoined,
                    allNodes
                )
            }
        } else {
            childDtos
                .flatMap { it.containedInternalDependencies.entries }
                .groupBy({ it.key }, { it.value })
                .mapValues { (targetId, edgeInfos) ->
                    edgeInfos.sum(level ?: 0, allNodes[targetId]?.level ?: 0)
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
        allNodes: Map<String, GraphNode>
    ): EdgeInfoDto {
        val sourceLevel = level ?: 0
        val targetNode = allNodes[this]
        val targetLevel = targetNode?.level ?: 0
        val isPointingUpwards = sourceLevel <= targetLevel

        return EdgeInfoDto(
            isCyclic = cyclicEdges?.contains(this) ?: false,
            weight = 1,
            type = type,
            isPointingUpwards = isPointingUpwards
        )
    }

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
