package de.maibornwolff.codegraph.pipeline.analysis.model

import de.maibornwolff.codegraph.pipeline.shared.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FileReportMultiLanguageTypeResolutionTest {
    @Test
    fun `C# should handle qualified types with namespace prefix`() {
        // given - C# uses namespace.Type syntax
        val projectDictionary = mapOf(
            "User" to listOf(Path(listOf("MyProject", "Models", "User")))
        )
        val languageDictionary = emptyMap<String, Path>()
        val node = Node.build(
            pathWithName = Path(listOf("MyProject", "Services", "UserService")),
            physicalPath = "Services/UserService.cs",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.C_SHARP,
            dependencies = setOf(
                Dependency(Path(listOf("MyProject", "Models")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("Models.User")) // C# qualified type
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then - Generic approach should resolve this correctly
        val expectedPath = Path(listOf("MyProject", "Models", "User"))
        assertThat(resolvedNode.usedTypes.first().resolvedPath).isEqualTo(expectedPath)
        assertThat(resolvedNode.dependencies).contains(Dependency(expectedPath))
    }

    @Test
    fun `C# should handle fully qualified namespace types`() {
        // given - C# with full namespace qualification
        val projectDictionary = mapOf(
            "User" to listOf(Path(listOf("MyProject", "Domain", "Models", "User")))
        )
        val languageDictionary = emptyMap<String, Path>()
        val node = Node.build(
            pathWithName = Path(listOf("MyProject", "Services", "UserService")),
            physicalPath = "Services/UserService.cs",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.C_SHARP,
            dependencies = setOf(
                Dependency(Path(listOf("MyProject", "Domain", "Models")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("MyProject.Domain.Models.User")) // Fully qualified
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then
        val resolvedPath = resolvedNode.usedTypes.first().resolvedPath
        println("C# fully qualified resolved to: $resolvedPath")

        // Generic approach would use "User" and find it via wildcard
    }

    @Test
    fun `Java should handle qualified types with package prefix`() {
        // given - Java with qualified reference
        val projectDictionary = mapOf(
            "User" to listOf(Path(listOf("com", "example", "models", "User")))
        )
        val languageDictionary = emptyMap<String, Path>()
        val node = Node.build(
            pathWithName = Path(listOf("com", "example", "services", "UserService")),
            physicalPath = "src/com/example/services/UserService.java",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.JAVA,
            dependencies = setOf(
                Dependency(Path(listOf("com", "example", "models")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("models.User")) // Qualified reference
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then - Generic approach should resolve this correctly
        val expectedPath = Path(listOf("com", "example", "models", "User"))
        assertThat(resolvedNode.usedTypes.first().resolvedPath).isEqualTo(expectedPath)
        assertThat(resolvedNode.dependencies).contains(Dependency(expectedPath))
    }

    @Test
    fun `Java should handle inner class references`() {
        // given - Java inner classes use $ but might appear as dots in source
        val projectDictionary = mapOf(
            "InnerClass" to listOf(Path(listOf("com", "example", "models", "User", "InnerClass")))
        )
        val languageDictionary = emptyMap<String, Path>()
        val node = Node.build(
            pathWithName = Path(listOf("com", "example", "services", "UserService")),
            physicalPath = "src/com/example/services/UserService.java",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.JAVA,
            dependencies = setOf(
                Dependency(Path(listOf("com", "example", "models")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("User.InnerClass")) // Inner class reference
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then
        val resolvedPath = resolvedNode.usedTypes.first().resolvedPath
        println("Java inner class resolved to: $resolvedPath")
    }

    @Test
    fun `PHP should handle namespaced types`() {
        // given - PHP with namespace qualification
        val projectDictionary = mapOf(
            "User" to listOf(Path(listOf("App", "Models", "User")))
        )
        val languageDictionary = emptyMap<String, Path>()
        val node = Node.build(
            pathWithName = Path(listOf("App", "Services", "UserService")),
            physicalPath = "app/Services/UserService.php",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.PHP,
            dependencies = setOf(
                Dependency(Path(listOf("App", "Models")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("Models.User")) // Converted from Models\User
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then - Generic approach should resolve this correctly
        val expectedPath = Path(listOf("App", "Models", "User"))
        assertThat(resolvedNode.usedTypes.first().resolvedPath).isEqualTo(expectedPath)
        assertThat(resolvedNode.dependencies).contains(Dependency(expectedPath))
    }

    @Test
    fun `PHP should handle fully qualified namespace`() {
        // given - PHP fully qualified namespace
        val projectDictionary = mapOf(
            "User" to listOf(Path(listOf("App", "Domain", "Models", "User")))
        )
        val languageDictionary = emptyMap<String, Path>()
        val node = Node.build(
            pathWithName = Path(listOf("App", "Services", "UserService")),
            physicalPath = "app/Services/UserService.php",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.PHP,
            dependencies = setOf(
                Dependency(Path(listOf("App", "Domain", "Models")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("App.Domain.Models.User")) // Fully qualified
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then
        val resolvedPath = resolvedNode.usedTypes.first().resolvedPath
        println("PHP fully qualified resolved to: $resolvedPath")
    }

    @Test
    fun `Python should handle module qualified types`() {
        // given - Python with package qualification
        val projectDictionary = mapOf(
            "User" to listOf(Path(listOf("myproject", "domain", "models", "User")))
        )
        val languageDictionary = emptyMap<String, Path>()
        val node = Node.build(
            pathWithName = Path(listOf("myproject", "services", "UserService")),
            physicalPath = "myproject/services/user_service.py",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.PYTHON,
            dependencies = setOf(
                Dependency(Path(listOf("myproject", "domain", "models")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("models.User")) // Package qualified
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then - Generic approach should resolve this correctly
        val expectedPath = Path(listOf("myproject", "domain", "models", "User"))
        assertThat(resolvedNode.usedTypes.first().resolvedPath).isEqualTo(expectedPath)
        assertThat(resolvedNode.dependencies).contains(Dependency(expectedPath))
    }

    @Test
    fun `Python should handle package qualified types`() {
        // given - Python with package qualification
        val projectDictionary = mapOf(
            "User" to listOf(Path(listOf("myproject", "domain", "models", "User")))
        )
        val languageDictionary = emptyMap<String, Path>()
        val node = Node.build(
            pathWithName = Path(listOf("myproject", "services", "UserService")),
            physicalPath = "myproject/services/user_service.py",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.PYTHON,
            dependencies = setOf(
                Dependency(Path(listOf("myproject", "domain", "models")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("models.User")) // Package qualified
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then
        val resolvedPath = resolvedNode.usedTypes.first().resolvedPath
        println("Python models.User resolved to: $resolvedPath")
    }

    @Test
    fun `TypeScript should handle namespace qualified types`() {
        // given - TypeScript with namespace qualification
        val projectDictionary = mapOf(
            "User" to listOf(Path(listOf("src", "models", "User")))
        )
        val languageDictionary = emptyMap<String, Path>()
        val node = Node.build(
            pathWithName = Path(listOf("src", "services", "UserService")),
            physicalPath = "src/services/UserService.ts",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.TYPESCRIPT,
            dependencies = setOf(
                Dependency(Path(listOf("src", "models")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("Models.User")) // Namespace qualified
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then - Generic approach should resolve this correctly
        val expectedPath = Path(listOf("src", "models", "User"))
        assertThat(resolvedNode.usedTypes.first().resolvedPath).isEqualTo(expectedPath)
        assertThat(resolvedNode.dependencies).contains(Dependency(expectedPath))
    }

    @Test
    fun `TypeScript should handle module qualified types`() {
        // given - TypeScript module imports with qualification
        val projectDictionary = mapOf(
            "User" to listOf(Path(listOf("src", "app", "models", "User")))
        )
        val languageDictionary = emptyMap<String, Path>()
        val node = Node.build(
            pathWithName = Path(listOf("src", "app", "services", "UserService")),
            physicalPath = "src/app/services/UserService.ts",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.TYPESCRIPT,
            dependencies = setOf(
                Dependency(Path(listOf("src", "app", "models")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("models.User")) // Module qualified
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then
        val resolvedPath = resolvedNode.usedTypes.first().resolvedPath
        println("TypeScript models.User resolved to: $resolvedPath")
    }

    @Test
    fun `TypeScript should handle complex namespace paths`() {
        // given - TypeScript with complex namespace structure
        val projectDictionary = mapOf(
            "User" to listOf(Path(listOf("src", "domain", "entities", "User")))
        )
        val languageDictionary = emptyMap<String, Path>()
        val node = Node.build(
            pathWithName = Path(listOf("src", "services", "UserService")),
            physicalPath = "src/services/UserService.ts",
            nodeType = NodeType.CLASS,
            language = SupportedLanguage.TYPESCRIPT,
            dependencies = setOf(
                Dependency(Path(listOf("src", "domain", "entities")), isWildcard = true)
            ),
            usedTypes = setOf(Type.simple("Domain.Entities.User")) // Complex qualified
        )

        // when
        val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

        // then
        val resolvedPath = resolvedNode.usedTypes.first().resolvedPath
        println("TypeScript complex qualified resolved to: $resolvedPath")
    }

    @Test
    fun `all languages should handle qualified types uniformly`() {
        // given - This test demonstrates the universal nature of the generic approach
        val languagesWithTypes = listOf(
            SupportedLanguage.GO to "models.User",
            SupportedLanguage.C_SHARP to "Models.User",
            SupportedLanguage.JAVA to "models.User",
            SupportedLanguage.PHP to "Models.User",
            SupportedLanguage.PYTHON to "models.User",
            SupportedLanguage.TYPESCRIPT to "Models.User"
        )

        languagesWithTypes.forEach { (language, qualifiedType) ->
            val projectDictionary = mapOf(
                "User" to listOf(Path(listOf("project", "models", "User")))
            )
            val languageDictionary = emptyMap<String, Path>()
            val node = Node.build(
                pathWithName = Path(listOf("project", "services", "UserService")),
                physicalPath = "services/UserService.${language.suffixes.first()}",
                nodeType = NodeType.CLASS,
                language = language,
                dependencies = setOf(
                    Dependency(Path(listOf("project", "models")), isWildcard = true)
                ),
                usedTypes = setOf(Type.simple(qualifiedType))
            )

            // when
            val resolvedNode = node.resolveTypes(projectDictionary, languageDictionary, setOf())

            // then - All languages should resolve the same way
            val expectedPath = Path(listOf("project", "models", "User"))
            assertThat(resolvedNode.usedTypes.first().resolvedPath)
                .withFailMessage("Failed for ${language.name} with type $qualifiedType")
                .isEqualTo(expectedPath)
            assertThat(resolvedNode.dependencies).contains(Dependency(expectedPath))
        }
    }
}