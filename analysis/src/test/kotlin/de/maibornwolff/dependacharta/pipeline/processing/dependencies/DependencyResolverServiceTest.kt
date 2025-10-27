package de.maibornwolff.codegraph.pipeline.processing.dependencies

import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency
import de.maibornwolff.codegraph.pipeline.analysis.model.FileReport
import de.maibornwolff.codegraph.pipeline.analysis.model.Node
import de.maibornwolff.codegraph.pipeline.analysis.model.NodeDependencies
import de.maibornwolff.codegraph.pipeline.analysis.model.NodeType
import de.maibornwolff.codegraph.pipeline.analysis.model.Path
import de.maibornwolff.codegraph.pipeline.analysis.model.Type
import de.maibornwolff.codegraph.pipeline.analysis.model.TypeOfUsage
import de.maibornwolff.codegraph.pipeline.analysis.model.build
import de.maibornwolff.codegraph.pipeline.processing.cycledetection.model.NodeInformation
import de.maibornwolff.codegraph.pipeline.shared.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DependencyResolverServiceTest {
    @Test
    fun `should resolve used type from same file`() {
        // given
        val aClass = Node.build(
            pathWithName = Path(listOf("de", "mw", "A")),
            physicalPath = "de/mw/A.java",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.JAVA,
            dependencies = setOf(
                Dependency(Path(listOf("de", "mw")), true),
            ),
            usedTypes = setOf(Type("B", TypeOfUsage.USAGE, emptyList())),
        )

        val bClass = Node.build(
            pathWithName = Path(listOf("de", "mw", "B")),
            physicalPath = "de/mw/B.java",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.JAVA,
            dependencies = setOf(
                Dependency(Path(listOf("de", "mw")), true),
            ),
            usedTypes = setOf(),
        )

        val report = FileReport(
            nodes = listOf(aClass, bClass),
        )

        // when
        val resolvedNodes = DependencyResolverService.resolveNodes(listOf(report))

        // then
        val expected = aClass.copy(
            resolvedNodeDependencies = NodeDependencies(
                internalDependencies = setOf(Dependency(bClass.pathWithName)),
                externalDependencies = emptySet()
            )
        )
        val actual = resolvedNodes.first { it.pathWithName.parts.last() == "A" }

        assertEquals(actual.resolvedNodeDependencies.internalDependencies, expected.resolvedNodeDependencies.internalDependencies)
    }

    @Test
    fun `should resolve used type from relative wildcard dependency`() {
        // given
        val address = Node.build(
            pathWithName = Path(listOf("cpu", "aarch64", "assembler_aarch64_inline_hpp", "Address")),
            physicalPath = "cpu/aarch64/assembler_aarch64.inline.hpp",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.CPP,
            dependencies = setOf(
                Dependency(Path(listOf("asm", "assembler_inline_hpp")), true),
            ),
            usedTypes = setOf(Type("Assembler", TypeOfUsage.USAGE, emptyList())),
        )
        val assembler = Node.build(
            pathWithName = Path(listOf("foo", "bar", "anywhere", "asm", "assembler_inline_hpp", "Assembler")),
            physicalPath = "foo/bar/anywhere/asm/assembler.inline.hpp",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.CPP,
            dependencies = setOf(),
            usedTypes = setOf()
        )
        val anotherAssembler = Node.build(
            pathWithName = Path(listOf("de", "somewhere", "cpu", "assembler_aarch64_inline_hpp", "Assembler")),
            physicalPath = "de/somewhere/cpu/assembler_aarch64.inline.hpp",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.CPP,
            dependencies = setOf(),
            usedTypes = setOf()
        )
        val report = FileReport(
            nodes = listOf(anotherAssembler, address, assembler),
        )

        // when
        val resolvedNodes = DependencyResolverService.resolveNodes(listOf(report))

        // then
        val expected = address.copy(
            resolvedNodeDependencies = NodeDependencies(
                internalDependencies = setOf(Dependency(assembler.pathWithName)),
                externalDependencies = emptySet()
            )
        )
        val actual = resolvedNodes.first { it.pathWithName.parts.last() == "Address" }

        assertEquals(actual.resolvedNodeDependencies.internalDependencies, expected.resolvedNodeDependencies.internalDependencies)
    }

    @Test
    fun `Transforms lists of nodes to correct dictionary`() {
        // given
        val node1 = Node.build(
            pathWithName = Path(listOf("de", "maibornwolff", "main")),
            physicalPath = "de/maibornwolff/main"
        )

        val node2 = Node.build(
            pathWithName = Path(listOf("de", "maibornwolff", "helper")),
            physicalPath = "de/maibornwolff/helper"
        )

        // when
        val dictionary = DependencyResolverService.getDictionary(listOf(node1, node2))

        // then
        val expectedDictionary = mapOf("main" to listOf(node1.pathWithName), "helper" to listOf(node2.pathWithName))
        assertThat(dictionary).isEqualTo(expectedDictionary)
    }

    @Test
    fun `Transforms lists of nodes to correct known paths`() {
        // given
        val node1 = Node.build(
            pathWithName = Path(listOf("de", "maibornwolff", "main")),
            physicalPath = "de/maibornwolff/main",
        )

        val node2 = Node.build(
            pathWithName = Path(listOf("de", "maibornwolff", "helper")),
            physicalPath = "de/maibornwolff/helper"
        )

        // when
        val dictionary = DependencyResolverService.getKnownNodePaths(listOf(node1, node2))

        // then
        val expectedDictionary = setOf(node1.pathWithName.withDots(), node2.pathWithName.withDots())
        assertThat(dictionary).isEqualTo(expectedDictionary)
    }

    @Test
    fun `Correctly resolves nodes with dictionary and known paths`() {
        // given
        val node1 = Node.build(
            pathWithName = Path(listOf("de", "maibornwolff", "main")),
            physicalPath = "de/maibornwolff/main",
        )

        val node2 = Node.build(
            pathWithName = Path(listOf("de", "maibornwolff", "helper")),
            physicalPath = "de/maibornwolff/helper"
        )

        val fileReport1 = FileReport(listOf(node1))
        val fileReport2 = FileReport(listOf(node2))

        // when
        val resolvedNodes = DependencyResolverService.resolveNodes(listOf(fileReport1, fileReport2))

        // then
        val expectedResolvedNodes = resolvedNodes.map { it.pathWithName }
        assertThat(resolvedNodes).hasSize(2)
        assertThat(expectedResolvedNodes).containsExactlyInAnyOrder(node1.pathWithName, node2.pathWithName)
    }

    @Test
    fun `Maps Node to NodeInformation`() {
        // given
        val node = Node.build(
            pathWithName = Path(listOf("de", "maibornwolff", "main")),
            resolvedNodeDependencies = NodeDependencies(
                setOf(Dependency(Path(listOf("de", "maibornwolff", "helper")))),
                emptySet()
            )
        )

        // when
        val nodeInformation = DependencyResolverService.mapNodeInfo(node)

        // then
        val expected = NodeInformation(
            id = "de.maibornwolff.main",
            dependencies = setOf("de.maibornwolff.helper")
        )

        assertThat(nodeInformation).isEqualTo(expected)
    }
}
