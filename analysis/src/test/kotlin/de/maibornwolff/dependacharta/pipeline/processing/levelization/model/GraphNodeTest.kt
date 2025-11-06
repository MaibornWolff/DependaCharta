package de.maibornwolff.dependacharta.pipeline.processing.levelization.model

import de.maibornwolff.dependacharta.pipeline.analysis.model.TypeOfUsage
import de.maibornwolff.dependacharta.pipeline.processing.model.EdgeInfoDto
import de.maibornwolff.dependacharta.pipeline.processing.model.ProjectNodeDto
import de.maibornwolff.dependacharta.pipeline.processing.model.build
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GraphNodeTest {
    @Test
    fun `Converts GraphNode without children to ProjectNodeDto`() {
        // given
        val graphNode = GraphNodeBuilder(id = "my.node", dependencies = setOf("de.maibornwolff.main")).build()

        // when
        val projectNodeDto = graphNode.toProjectNodeDto(emptyMap(), mapOf(graphNode.id to graphNode))

        // then
        val expected = ProjectNodeDto.build(
            leafId = graphNode.id,
            name = "node",
            containedLeaves = setOf(graphNode.id),
            containedInternalDependencies = mapOf("de.maibornwolff.main" to EdgeInfoDto(false, 1, TypeOfUsage.USAGE.rawValue, true))
        )

        assertThat(projectNodeDto).isEqualTo(expected)
    }

    @Test
    fun `Converts GraphNode with children to ProjectNodeDto`() {
        // given
        val parentId = "node"
        val graphNode = GraphNodeBuilder(id = parentId)
            .withChildren(
                GraphNodeBuilder(
                    id = "child1",
                    parent = parentId,
                    dependencies = setOf("de.maibornwolff.main")
                ).build(),
                GraphNodeBuilder(
                    id = "child2",
                    parent = parentId,
                    dependencies = setOf("de.maibornwolff.main")
                ).build()
            ).build()

        // when
        val allNodesMap = mapOf(
            graphNode.id to graphNode,
            "node.child1" to graphNode.children[0],
            "node.child2" to graphNode.children[1]
        )
        val projectNodeDto = graphNode.toProjectNodeDto(emptyMap(), allNodesMap)

        // then
        val expected = ProjectNodeDto.build(
            name = "node",
            children = setOf(
                ProjectNodeDto.build(
                    leafId = "node.child1",
                    name = "child1",
                    containedLeaves = setOf("node.child1"),
                    containedInternalDependencies = mapOf("de.maibornwolff.main" to EdgeInfoDto(false, 1, TypeOfUsage.USAGE.rawValue, true))
                ),
                ProjectNodeDto.build(
                    leafId = "node.child2",
                    name = "child2",
                    containedLeaves = setOf("node.child2"),
                    containedInternalDependencies = mapOf("de.maibornwolff.main" to EdgeInfoDto(false, 1, TypeOfUsage.USAGE.rawValue, true))
                )
            ),
            containedLeaves = setOf("node.child1", "node.child2"),
            containedInternalDependencies = mapOf("de.maibornwolff.main" to EdgeInfoDto(false, 2, TypeOfUsage.USAGE.rawValue, true))
        )

        assertThat(projectNodeDto).isEqualTo(expected)
    }

    @Test
    fun `Converts GraphNode with cycles and children to ProjectNodeDto`() {
        // given
        val parentId = "node"
        val graphNode = GraphNodeBuilder(id = parentId)
            .withChildren(
                GraphNodeBuilder(
                    id = "child1",
                    parent = parentId,
                    dependencies = setOf("de.maibornwolff.main")
                ).build(),
                GraphNodeBuilder(
                    id = "child2",
                    parent = parentId,
                    dependencies = setOf("de.maibornwolff.main")
                ).build()
            ).build()

        val cycles = mapOf("node.child2" to setOf("de.maibornwolff.main"))

        // when
        val allNodesMap = mapOf(
            graphNode.id to graphNode,
            "node.child1" to graphNode.children[0],
            "node.child2" to graphNode.children[1]
        )
        val projectNodeDto = graphNode.toProjectNodeDto(cycles, allNodesMap)

        // then
        val expected = ProjectNodeDto.build(
            name = "node",
            children = setOf(
                ProjectNodeDto.build(
                    leafId = "node.child1",
                    name = "child1",
                    containedLeaves = setOf("node.child1"),
                    containedInternalDependencies = mapOf("de.maibornwolff.main" to EdgeInfoDto(false, 1, TypeOfUsage.USAGE.rawValue, true))
                ),
                ProjectNodeDto.build(
                    leafId = "node.child2",
                    name = "child2",
                    containedLeaves = setOf("node.child2"),
                    containedInternalDependencies = mapOf("de.maibornwolff.main" to EdgeInfoDto(true, 1, TypeOfUsage.USAGE.rawValue, true))
                )
            ),
            containedLeaves = setOf("node.child1", "node.child2"),
            containedInternalDependencies = mapOf("de.maibornwolff.main" to EdgeInfoDto(true, 2, TypeOfUsage.USAGE.rawValue, true))
        )

        assertThat(projectNodeDto).isEqualTo(expected)
    }
}
