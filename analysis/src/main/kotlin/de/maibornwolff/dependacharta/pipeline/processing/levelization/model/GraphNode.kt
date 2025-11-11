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
    companion object {
        /**
         * Finds a node by its ID in the tree.
         */
        fun findNodeById(
            root: GraphNode,
            id: String
        ): GraphNode? {
            if (root.id == id) return root
            for (child in root.children) {
                val found = findNodeById(child, id)
                if (found != null) return found
            }
            return null
        }

        /**
         * Gets all ancestors of a node, including the node itself.
         * Returns a list starting with the node and ending with the root.
         */
        fun getAncestors(
            node: GraphNode,
            root: GraphNode
        ): List<GraphNode> {
            val ancestors = mutableListOf(node)
            var current = node
            while (current.parent != null) {
                val parent = findNodeById(root, current.parent!!)
                    ?: throw IllegalStateException("Parent ${current.parent} not found for node ${current.id}")
                ancestors.add(parent)
                current = parent
            }
            return ancestors
        }

        /**
         * Finds siblings under the lowest common ancestor.
         * Returns a pair of (sourceAncestor, targetAncestor) that are siblings.
         */
        fun findSiblingsUnderLowestCommonAncestor(
            source: GraphNode,
            target: GraphNode,
            root: GraphNode
        ): Pair<GraphNode, GraphNode> {
            val sourceAncestors = getAncestors(source, root)
            val targetAncestors = getAncestors(target, root)

            for (sourceAncestor in sourceAncestors) {
                for (targetAncestor in targetAncestors) {
                    // Check if they share the same parent
                    if (sourceAncestor.parent != null &&
                        targetAncestor.parent != null &&
                        sourceAncestor.parent == targetAncestor.parent
                    ) {
                        return Pair(sourceAncestor, targetAncestor)
                    }
                }
            }

            throw IllegalStateException("No common ancestor found for ${source.id} and ${target.id}")
        }

        /**
         * Calculates if an edge points upwards (violates normal dependency flow).
         * An edge points upwards when sourceLevel <= targetLevel under their lowest common ancestor.
         */
        fun calculateIsPointingUpwards(
            sourceId: String,
            targetId: String,
            root: GraphNode
        ): Boolean {
            val source = findNodeById(root, sourceId)
                ?: throw IllegalStateException("Source node $sourceId not found")
            val target = findNodeById(root, targetId)
                ?: throw IllegalStateException("Target node $targetId not found")

            val (sourceAncestor, targetAncestor) = findSiblingsUnderLowestCommonAncestor(source, target, root)
            return sourceAncestor.level!! <= targetAncestor.level!!
        }
    }

    fun toProjectNodeDto(
        cyclicEdgesByLeaf: Map<String, Set<String>>,
        root: GraphNode = this
    ): ProjectNodeDto {
        val isLeaf = children.isEmpty()
        val childDtos = children.map { it.toProjectNodeDto(cyclicEdgesByLeaf, root) }.toSet()

        val internalDependencies = if (isLeaf) {
            dependencies.associateWith { dependency ->
                val edgeTypes = edges.filter { it.target == dependency }.map { it.type }.toSet()
                val edgeTypesJoined = edgeTypes.joinToString(",")
                dependency.toEdgeInfoDto(
                    cyclicEdges = cyclicEdgesByLeaf[id],
                    type = if (edgeTypes.isEmpty()) TypeOfUsage.USAGE.rawValue else edgeTypesJoined,
                    sourceId = id,
                    root = root
                )
            }
        } else {
            childDtos
                .flatMap { it.containedInternalDependencies.entries }
                .groupBy({ it.key }, { it.value })
                .mapValues { it.value.sum() }
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
        sourceId: String,
        root: GraphNode
    ): EdgeInfoDto {
        val isPointingUpwards = try {
            calculateIsPointingUpwards(sourceId, this, root)
        } catch (e: IllegalStateException) {
            // If we can't calculate (e.g., no common ancestor), default to false
            false
        }

        return EdgeInfoDto(
            isCyclic = cyclicEdges?.contains(this) ?: false,
            weight = 1,
            type = type,
            isPointingUpwards = isPointingUpwards
        )
    }

    private fun List<EdgeInfoDto>.sum() =
        EdgeInfoDto(
            isCyclic = any { it.isCyclic },
            weight = sumOf { it.weight },
            type = map { it.type }.toSet().joinToString(",")
        )
}
