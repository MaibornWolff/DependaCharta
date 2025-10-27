package de.maibornwolff.dependacharta.pipeline.processing.levelization

import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.processing.levelization.model.GraphEdge
import de.maibornwolff.dependacharta.pipeline.processing.levelization.model.GraphNode

fun Collection<Node>.toGraphNodes(): List<GraphNode> = buildTree(null, 0, this).toList()

private fun buildTree(
    parentId: String?,
    pathLevel: Int,
    nodes: Collection<Node>,
): Set<GraphNode> {
    val nextLevel = pathLevel + 1
    val leaves = nodes.filter { it.pathWithName.parts.size == nextLevel }
    val leafNodes = leaves.map { leaf ->
        val leafId = leaf.pathWithName.withDots()
        GraphNode(
            id = leafId,
            parent = parentId,
            children = listOf(),
            dependencies = leaf.resolvedNodeDependencies.internalDependencies
                .map { it.withDots() }
                .toSet(),
            edges = leaf.resolvedNodeDependencies.internalDependencies
                .map { GraphEdge(leafId, it.withDots(), 1, it.type.rawValue) }
                .toSet()
        )
    }
    val subTreeRoots = nodes.filter { it.pathWithName.parts.size > nextLevel }
    val byPath = subTreeRoots.groupBy { it.pathWithName.parts[pathLevel] }
    val subTrees = byPath.map { (subTreePath, subTreeNodes) ->
        val nodeId = if (!parentId.isNullOrEmpty()) "$parentId.$subTreePath" else subTreePath
        val children = buildTree(nodeId, nextLevel, subTreeNodes).toList()
        GraphNode(
            id = nodeId,
            parent = parentId,
            children = children,
            edges = children.flatMap { it.edges }.toSet()
        )
    }
    return (leafNodes + subTrees).toSet()
}
