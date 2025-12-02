package de.maibornwolff.dependacharta.pipeline.analysis.analyzers

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin.KotlinAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinAnalyzerTest {
    @Test
    fun `should extract property types correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
                private val name: String = ""
                private var age: Int = 0
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).containsAll(
            listOf(Type.simple("String"), Type.simple("Int"))
        )
    }

    @Test
    fun `should extract function return types correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
                fun getName(): String {
                    return "name"
                }

                fun getAge(): Int {
                    return 42
                }
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).containsAll(
            listOf(Type.simple("String"), Type.simple("Int"))
        )
    }

    @Test
    fun `should extract function parameter types correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
                fun setName(name: String) {
                }

                fun setAge(age: Int) {
                }
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).containsAll(
            listOf(Type.simple("String"), Type.simple("Int"))
        )
    }

    @Test
    fun `should extract constructor parameter types correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest(val name: String, var age: Int)
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).containsAll(
            listOf(Type.simple("String"), Type.simple("Int"))
        )
    }

    @Test
    fun `should extract used annotation types correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            import kotlin.Deprecated
            import kotlin.Suppress

            class KotlinAnalyzerTest {
                @Deprecated("old")
                @Suppress("unused")
                fun setName(name: String) {
                }
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).containsAll(
            listOf(Type.simple("Deprecated"), Type.simple("Suppress"))
        )
    }

    @Test
    fun `should create node for each class and interface in a given file`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
            }

            interface TestInterface {
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val nodes = report.nodes
        assertEquals(2, nodes.size)
        assertEquals("KotlinAnalyzerTest", nodes[0].pathWithName.parts.last())
        assertEquals("TestInterface", nodes[1].pathWithName.parts.last())
    }

    @Test
    fun `should create node for object declaration`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            object MySingleton {
                val name: String = "singleton"
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val nodes = report.nodes
        assertEquals(1, nodes.size)
        assertEquals("MySingleton", nodes[0].pathWithName.parts.last())
        assertEquals(NodeType.CLASS, nodes[0].nodeType)
    }

    @Test
    fun `should extract types of generics correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
                private val names: List<String> = emptyList()
                private val nameToAge: Map<String, Int> = emptyMap()
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).containsAll(
            listOf(
                Type.generic("List", listOf(Type.simple("String"))),
                Type.generic("Map", listOf(Type.simple("String"), Type.simple("Int")))
            )
        )
    }

    @Test
    fun `should parse imports and add them to the node's dependencies`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            import kotlin.collections.List
            import kotlin.collections.Map

            class KotlinAnalyzerTest {
                private val names: List<String> = emptyList()
                private val nameToAge: Map<String, Int> = emptyMap()
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val dependencies = report.nodes.first().dependencies
        assertEquals(3, dependencies.size)
        assertThat(dependencies).containsExactlyInAnyOrder(
            Dependency(Path.fromStringWithDots("de.maibornwolff.dependacharta.analysis.analyzers"), true),
            Dependency(Path.fromStringWithDots("kotlin.collections.List")),
            Dependency(Path.fromStringWithDots("kotlin.collections.Map"))
        )
    }

    @Test
    fun `should add implicit dependency on the class's package`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val dependencies = report.nodes.first().dependencies
        assertEquals(1, dependencies.size)
        assertThat(dependencies).containsExactlyInAnyOrder(
            Dependency(Path.fromStringWithDots("de.maibornwolff.dependacharta.analysis.analyzers"), true)
        )
    }

    @Test
    fun `should extract the superclass of a class correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest : SuperClass() {
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).contains(
            Type.simple("SuperClass")
        )
    }

    @Test
    fun `should extract the interfaces of a class correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest : Interface1, Interface2 {
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).containsAll(
            listOf(Type.simple("Interface1"), Type.simple("Interface2"))
        )
    }

    @Test
    fun `should extract superclass and interfaces together`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest : SuperClass(), Interface1, Interface2 {
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).containsAll(
            listOf(Type.simple("SuperClass"), Type.simple("Interface1"), Type.simple("Interface2"))
        )
    }

    @Test
    fun `should extract constructor calls to usedTypes correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
                fun someMethod() {
                    val instance = SomeClass()
                }
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).contains(
            Type.simple("SomeClass")
        )
    }

    @Test
    fun `should extract static method calls to usedTypes correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
                fun someMethod() {
                    SomeClass.someStaticMethod()
                }
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).contains(
            Type.simple("SomeClass")
        )
    }

    @Test
    fun `should extract companion object access to usedTypes correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
                fun someMethod() {
                    val value = SomeClass.CONSTANT
                }
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).contains(
            Type.simple("SomeClass")
        )
    }

    @Test
    fun `should handle nullable types correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
                private val name: String? = null
                private val age: Int? = null
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).containsAll(
            listOf(Type.simple("String"), Type.simple("Int"))
        )
    }

    @Test
    fun `should handle data class correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            data class Person(val name: String, val age: Int)
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val nodes = report.nodes
        assertEquals(1, nodes.size)
        assertEquals("Person", nodes[0].pathWithName.parts.last())
        assertEquals(NodeType.CLASS, nodes[0].nodeType)

        val usedTypes = nodes[0].usedTypes
        assertThat(usedTypes).containsAll(
            listOf(Type.simple("String"), Type.simple("Int"))
        )
    }

    @Test
    fun `should handle wildcard imports correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            import kotlin.collections.*

            class KotlinAnalyzerTest {
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val dependencies = report.nodes.first().dependencies
        assertThat(dependencies).contains(
            Dependency(Path.fromStringWithDots("kotlin.collections"), true)
        )
    }

    @Test
    fun `should detect interface node type correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            interface MyInterface {
                fun doSomething()
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val nodes = report.nodes
        assertEquals(1, nodes.size)
        assertEquals(NodeType.INTERFACE, nodes[0].nodeType)
    }

    @Test
    fun `should handle generic inheritance correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest : BaseClass<String>() {
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).contains(
            Type.generic("BaseClass", listOf(Type.simple("String")))
        )
    }

    @Test
    fun `should handle multiple classes in one file`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class ClassA {
                private val b: ClassB? = null
            }

            class ClassB {
                private val a: ClassA? = null
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        assertEquals(2, report.nodes.size)

        val classA = report.nodes[0]
        assertEquals("ClassA", classA.pathWithName.parts.last())
        assertThat(classA.usedTypes).contains(Type.simple("ClassB"))

        val classB = report.nodes[1]
        assertEquals("ClassB", classB.pathWithName.parts.last())
        assertThat(classB.usedTypes).contains(Type.simple("ClassA"))
    }

    @Test
    fun `should set correct language for nodes`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        assertEquals(SupportedLanguage.KOTLIN, report.nodes.first().language)
    }

    @Test
    fun `should set correct physical path for nodes`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./my/path/File.kt", kotlinCode)).analyze()

        assertEquals("./my/path/File.kt", report.nodes.first().physicalPath)
    }

    @Test
    fun `should handle empty file without declarations`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        assertEquals(0, report.nodes.size)
    }

    @Test
    fun `should extract nested generic types correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class KotlinAnalyzerTest {
                private val data: Map<String, List<Int>> = emptyMap()
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).contains(
            Type.generic(
                "Map",
                listOf(
                    Type.simple("String"),
                    Type.generic("List", listOf(Type.simple("Int")))
                )
            )
        )
    }

    @Test
    fun `should handle enum class correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            enum class Status {
                ACTIVE, INACTIVE, PENDING
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val nodes = report.nodes
        assertEquals(1, nodes.size)
        assertThat(
            nodes
                .first()
                .pathWithName.parts
                .last()
        ).isEqualTo("Status")
        assertThat(nodes.first().nodeType).isEqualTo(NodeType.ENUM)
    }

    @Test
    fun `should handle enum class implementing interface`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            enum class Status : Comparable<Status> {
                ACTIVE, INACTIVE
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).contains(
            Type.generic("Comparable", listOf(Type.simple("Status")))
        )
    }

    @Test
    fun `should handle sealed class correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            sealed class Result {
                data class Success(val data: String) : Result()
                data class Error(val message: String) : Result()
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val nodes = report.nodes
        assertEquals(3, nodes.size)
        assertThat(nodes.map { it.pathWithName.parts.last() })
            .containsExactly("Result", "Success", "Error")
    }

    @Test
    fun `should handle nested class correctly`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class Outer {
                class Inner {
                    val value: String = ""
                }
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val nodes = report.nodes
        assertEquals(2, nodes.size)
        assertThat(nodes.map { it.pathWithName.parts.last() })
            .containsExactly("Outer", "Inner")
    }

    @Test
    fun `should handle inner class with dependency on outer`() {
        val kotlinCode = """
            package de.maibornwolff.dependacharta.analysis.analyzers

            class Outer {
                inner class Inner {
                    fun getOuter(): Outer = this@Outer
                }
            }
        """.trimIndent()

        val report = KotlinAnalyzer(FileInfo(SupportedLanguage.KOTLIN, "./path", kotlinCode)).analyze()

        val nodes = report.nodes
        assertEquals(2, nodes.size)

        val innerClass = nodes.last()
        assertThat(innerClass.pathWithName.parts.last()).isEqualTo("Inner")
        assertThat(innerClass.usedTypes).contains(Type.simple("Outer"))
    }
}
