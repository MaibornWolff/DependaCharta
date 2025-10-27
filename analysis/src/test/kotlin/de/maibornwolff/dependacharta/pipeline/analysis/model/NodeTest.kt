package de.maibornwolff.dependacharta.pipeline.analysis.model

import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class NodeTest {
    @Test
    fun `should resolve types of usedTypes correctly`() {
        // given
        val bookPath = Path(listOf("de", "maibornwolff", "dependacharta", "analysis", "Model", "Book"))
        val stringPath = Path(listOf("java", "lang", "String"))
        val intPath = Path(listOf("int"))

        val projectDictionary = mapOf(
            "Book" to listOf(bookPath),
        )

        val languageDictionary = mapOf(
            "String" to stringPath,
            "int" to intPath
        )

        val node = Node(
            pathWithName = Path(listOf("de", "maibornwolff", "dependacharta", "analysis", "analyzers", "JavaAnalyzerTest")),
            physicalPath = "./path",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.JAVA,
            dependencies = setOf(Dependency(bookPath)),
            usedTypes = setOf(
                Type.simple("String"),
                Type.simple("int"),
                Type.simple("Book")
            )
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then
        val usedTypes = resolvedNode.usedTypes
        val paths = usedTypes.mapNotNull { it.resolvedPath }
        Assertions.assertThat(paths).containsExactlyInAnyOrder(stringPath, intPath, bookPath)
    }

    @Test
    fun `should resolve scoped types of usedTypes correctly`() {
        // given
        val bookPath = Path(listOf("de", "maibornwolff", "dependacharta", "analysis", "Model", "Book"))
        val stringPath = Path(listOf("java", "lang", "String"))
        val intPath = Path(listOf("int"))

        val projectDictionary = mapOf(
            "Book" to listOf(bookPath),
        )

        val languageDictionary = mapOf(
            "String" to stringPath,
            "int" to intPath
        )

        val node = Node(
            pathWithName = Path(listOf("de", "maibornwolff", "dependacharta", "analysis", "analyzers", "JavaAnalyzerTest")),
            physicalPath = "./path",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.JAVA,
            dependencies = setOf(Dependency(bookPath)),
            usedTypes = setOf(
                Type.simple("String"),
                Type.simple("int"),
                Type.simple("Model.Book")
            )
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then
        val usedTypes = resolvedNode.usedTypes
        val paths = usedTypes.mapNotNull { it.resolvedPath }
        Assertions.assertThat(paths).containsExactlyInAnyOrder(stringPath, intPath, bookPath)
        Assertions.assertThat(usedTypes.map { it.name }).containsExactlyInAnyOrder("String", "int", "Book")
    }

    @Test
    fun `should resolve fully qualified types of usedTypes correctly`() {
        // given
        val bookPath = Path("analysis.Model.Book".split("."))
        val anotherBookPath = Path("analysis.OtherModel.Book".split("."))
        val projectDictionary = mapOf("Book" to listOf(bookPath, anotherBookPath))
        val node = Node(
            pathWithName = Path(listOf("de")),
            physicalPath = "./path",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.JAVA,
            dependencies = emptySet(),
            usedTypes = setOf(
                Type.simple("analysis.Model.Book")
            )
        )

        // when
        val languageDictionary = emptyMap<String, Path>()
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then
        val usedTypes = resolvedNode.usedTypes
        val paths = usedTypes.mapNotNull { it.resolvedPath }
        Assertions.assertThat(paths).containsExactlyInAnyOrder(bookPath)
        Assertions.assertThat(usedTypes.map { it.name }).containsExactlyInAnyOrder("Book")
    }

    @Test
    fun `should resolve partly qualified types of usedTypes correctly`() {
        // given
        val bookPath = Path("analysis.Model.Book".split("."))
        val anotherBookPath = Path("analysis.OtherModel.Book".split("."))
        val analysisPath = Path("analysis".split("."))
        val projectDictionary = mapOf("Book" to listOf(bookPath, anotherBookPath))
        val node = Node(
            pathWithName = Path(listOf("de")),
            physicalPath = "./path",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.JAVA,
            dependencies = setOf(Dependency(analysisPath, true)),
            usedTypes = setOf(
                Type.simple("OtherModel.Book")
            )
        )

        // when
        val languageDictionary = emptyMap<String, Path>()
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then
        val usedTypes = resolvedNode.usedTypes
        val paths = usedTypes.mapNotNull { it.resolvedPath }
        Assertions.assertThat(paths).containsExactlyInAnyOrder(anotherBookPath)
        Assertions.assertThat(usedTypes.map { it.name }).containsExactlyInAnyOrder("Book")
    }
}
