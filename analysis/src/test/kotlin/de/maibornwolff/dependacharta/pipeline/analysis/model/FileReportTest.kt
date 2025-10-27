package de.maibornwolff.codegraph.pipeline.analysis.model

import de.maibornwolff.codegraph.pipeline.processing.model.EdgeInfoDto
import de.maibornwolff.codegraph.pipeline.processing.model.LeafInformationDto
import de.maibornwolff.codegraph.pipeline.shared.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FileReportTest {
    @Test
    fun `resolveTypes should find project and filter out language dependencies`() {
        // given
        val projectDictionary = mapOf(
            "TypeA" to listOf(Path(listOf("com", "example", "TypeA"))),
            "TypeB" to listOf(Path(listOf("com", "example", "TypeB")))
        )
        val languageDictionary = mapOf(
            "TypeC" to Path(listOf("java", "lang", "TypeC"))
        )
        val node = Node(
            pathWithName = Path(listOf("com", "example", "Node")),
            physicalPath = "src/com/example/Node.java",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.JAVA,
            dependencies = setOf(
                Dependency(Path(listOf("com", "example", "TypeA"))),
                Dependency(Path(listOf("com", "example")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("TypeA"), Type.simple("TypeB"), Type.simple("TypeC")),
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then
        assertThat(resolvedNode.dependencies).containsExactlyInAnyOrder(
            Dependency(Path(listOf("com", "example", "TypeA"))),
            Dependency(Path(listOf("com", "example", "TypeB"))),
            Dependency(Path(listOf("com", "example")), isWildcard = true)
        )
    }

    @Test
    fun `resolveTypes should differentiate between internal and external dependencies`() {
        // given
        val internalPathTypeA = Path(listOf("com", "example", "TypeA"))
        val internalPathTypeB = Path(listOf("com", "example", "TypeB"))
        val projectDictionary = mapOf(
            "TypeA" to listOf(internalPathTypeA),
            "TypeB" to listOf(internalPathTypeB)
        )
        val externalPathTypeC = Path(listOf("some", "unknown", "TypeC"))
        val languageDictionary = mapOf<String, Path>()
        val node = Node(
            pathWithName = Path(listOf("com", "example", "Node")),
            physicalPath = "src/com/example/Node.java",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.JAVA,
            dependencies = setOf(
                Dependency(internalPathTypeA),
                Dependency(Path(listOf("com", "example")), isWildcard = true),
                Dependency(externalPathTypeC)
            ),
            usedTypes = setOf(Type.simple("TypeA"), Type.simple("TypeB"), Type.simple("TypeC")),
        )

        // when
        val resolvedNode = node.resolveTypes(
            projectDictionary,
            languageDictionary,
            setOf(internalPathTypeA.withDots(), internalPathTypeB.withDots())
        )

        // then
        assertThat(resolvedNode.resolvedNodeDependencies.internalDependencies).containsExactlyInAnyOrder(
            Dependency(internalPathTypeA),
            Dependency(internalPathTypeB)
        )
        assertThat(resolvedNode.resolvedNodeDependencies.externalDependencies).containsExactlyInAnyOrder(
            Dependency(externalPathTypeC)
        )
    }

    @Test
    fun `resolveTypes with unknown type should create only unknown type dependency`() {
        // given
        val unknownType = "UnknownType"
        val projectDictionary = emptyMap<String, List<Path>>()
        val languageDictionary = emptyMap<String, Path>()
        val node = Node(
            pathWithName = Path(listOf("com", "example", "Node")),
            physicalPath = "src/com/example/Node.java",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.JAVA,
            dependencies = emptySet(),
            usedTypes = setOf(Type.simple(unknownType))
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then
        assertThat(resolvedNode.dependencies).containsExactly(Dependency(Path.unknown(unknownType)))
        assertThat(resolvedNode.usedTypes).containsExactly(
            Type.simple(unknownType).copy(resolvedPath = Path.unknown(unknownType))
        )
    }

    @Test
    fun `maps to a LeafInformationDto without cyclic edges`() {
        // given
        val node = Node.build(
            pathWithName = Path(listOf("de", "maibornwolff", "main")),
            physicalPath = "de/maibornwolff/main",
            resolvedNodeDependencies = NodeDependencies(
                setOf(
                    Dependency(
                        path = Path(listOf("de", "maibornwolff", "main")),
                    )
                ),
                emptySet()
            )
        )
        val cyclicEdges = emptyMap<String, Set<String>>()

        // when
        val leafInformationDto = node.toLeafInformationDto(cyclicEdges)

        // then
        val expected = LeafInformationDto(
            id = "de.maibornwolff.main",
            name = "main",
            physicalPath = node.physicalPath,
            language = node.language.name,
            nodeType = node.nodeType.name,
            dependencies = mapOf("de.maibornwolff.main" to EdgeInfoDto(false, 1, TypeOfUsage.USAGE.rawValue))
        )

        assertThat(leafInformationDto).isEqualTo(expected)
    }

    @Test
    fun `maps to a LeafInformationDto with cyclic edges`() {
        // given
        val node = Node.build(
            pathWithName = Path(listOf("de", "maibornwolff", "main")),
            physicalPath = "de/maibornwolff/main",
            resolvedNodeDependencies = NodeDependencies(
                setOf(
                    Dependency(
                        path = Path(listOf("de", "maibornwolff", "helper")),
                    )
                ),
                emptySet()
            )
        )
        val cyclicEdges = mapOf("de.maibornwolff.main" to setOf("de.maibornwolff.helper"))

        // when
        val leafInformationDto = node.toLeafInformationDto(cyclicEdges)

        // then
        val expected = mapOf("de.maibornwolff.helper" to EdgeInfoDto(true, 1, TypeOfUsage.USAGE.rawValue))

        assertThat(leafInformationDto.dependencies).isEqualTo(expected)
    }
}
