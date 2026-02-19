package de.maibornwolff.dependacharta.pipeline.processing

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.TypescriptAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.processing.model.ProjectReportDto
import de.maibornwolff.dependacharta.pipeline.shared.LogLevel
import de.maibornwolff.dependacharta.pipeline.shared.Logger
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.assertTrue

class ProcessingPipelineTest {
    @AfterEach
    fun tearDown() {
        val projectReportFile = File("testresult/test.cg.json")
        projectReportFile.delete()
        File("testresult").delete()
    }

    @Test
    fun `processes Java Project correctly`() {
        // given
        val outputFileName = "test"
        val outputDirectoryName = "testresult"
        val fileReportFiles = File("src/test/resources/pipeline/projectreport/filereports")
            .walk()
            .filter { it.isFile && it.name.endsWith(".json") }
            .toSet()

        val fileReportJsons = fileReportFiles.map { it.readText(Charsets.UTF_8) }
        val fileReports = fileReportJsons.map { Json.decodeFromString<FileReport>(it) }

        // when
        ProcessingPipeline.run(outputFileName, outputDirectoryName, fileReports, false)

        // then
        val customJsonSerializer = Json { explicitNulls = false }
        val projectReport = getResult(customJsonSerializer)

        val expectedProjectReportFile = File("src/test/resources/pipeline/projectreport/java-example.cg.json")
        val expectedProjectReportJson = expectedProjectReportFile.readText(Charsets.UTF_8)
        val expectedProjectReport = customJsonSerializer.decodeFromString<ProjectReportDto>(expectedProjectReportJson)

        assertThat(projectReport)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*physicalPath$")
            .ignoringCollectionOrder()
            .isEqualTo(expectedProjectReport)
    }

    private fun getResult(customJsonSerializer: Json): ProjectReportDto {
        val projectReportFile = File("testresult/test.cg.json")
        val projectReportJson = projectReportFile.readText(Charsets.UTF_8)
        val projectReport = customJsonSerializer.decodeFromString<ProjectReportDto>(projectReportJson)
        return projectReport
    }

    @Test
    fun `should treat duplicate identical types as one - can happen when same type is declared in two separate files`() {
        // given
        val outputFileName = "test"
        val outputDirectoryName = "testresult"

        val pathWithNameRoot = "cpp/example/Root"
        val pathWithNameMyType = "cpp/example/MyType"
        val pathWithNameUsedType = "cpp/different/UsedType"

        val root = Node(
            pathWithName = Path
                .fromPhysicalPath(pathWithNameRoot)
                .withAlias(Path.fromPhysicalPath("$pathWithNameRoot.cpp")),
            language = SupportedLanguage.CPP,
            physicalPath = "$pathWithNameRoot.cpp",
            nodeType = NodeType.CLASS,
            dependencies = setOf(
                Dependency.simple("cpp", "example", "MyType_cpp"),
                Dependency.simple("cpp", "example", "MyType_h"),
            ),
            usedTypes = setOf(
                Type.simple("MyType")
            )
        )

        val nodeA = Node(
            pathWithName = Path
                .fromPhysicalPath(pathWithNameMyType)
                .withAlias(Path.fromPhysicalPath("$pathWithNameMyType.cpp")),
            language = SupportedLanguage.CPP,
            physicalPath = "$pathWithNameMyType.cpp",
            nodeType = NodeType.CLASS,
            dependencies = setOf(
                Dependency.simple("Bar", "FooType"),
                Dependency.simple("cpp", "example", "MyType_h"),
                Dependency.simple("cpp", "different", "UsedType_h"),
            ),
            usedTypes = setOf(Type.simple("UsedType"))
        )

        val nodeB = Node(
            pathWithName = Path
                .fromPhysicalPath(pathWithNameMyType)
                .withAlias(Path.fromPhysicalPath("$pathWithNameMyType.h")),
            language = SupportedLanguage.CPP,
            physicalPath = "$pathWithNameMyType.h",
            nodeType = NodeType.CLASS,
            dependencies = setOf(
                Dependency.simple("SomethingElse", "AnotherType"),
            ),
            usedTypes = emptySet()
        )

        val usedType = Node(
            pathWithName = Path.fromPhysicalPath(pathWithNameUsedType).withAlias(Path.fromPhysicalPath("$pathWithNameUsedType.h")),
            language = SupportedLanguage.CPP,
            physicalPath = "$pathWithNameUsedType.h",
            nodeType = NodeType.CLASS,
            dependencies = emptySet(),
            usedTypes = emptySet()
        )

        val fileReports = listOf(FileReport(listOf(root, nodeA)), FileReport(listOf(nodeB, usedType)))

        // when
        ProcessingPipeline.run(outputFileName, outputDirectoryName, fileReports, false)

        // then
        val result = getResult(Json { explicitNulls = false })

        val leaves = result.leaves
        assertThat(leaves).hasSize(3)
        val actual = leaves["cpp.example.MyType"] ?: throw AssertionError("Node not found")
        assertThat(actual.physicalPath).containsAnyOf(".h/.cpp", ".cpp/.h")

        val myTypeNode = result.projectTreeRoots
            .flatMap { it.children }
            .flatMap { it.children }
            .firstOrNull { grandGrandChild -> grandGrandChild.leafId == "cpp.example.MyType" }
            .let {
                it
                    ?: throw AssertionError("Node cpp.example.MyType not found in project tree roots")
            }
        assertThat(myTypeNode.containedInternalDependencies.keys).contains("cpp.different.UsedType")
        assertThat(result.leaves[myTypeNode.leafId]?.dependencies?.keys).contains("cpp.different.UsedType")
    }

    @Test
    fun `should report internal cycles as cycles`() {
        // given
        val outputFileName = "test"
        val outputDirectoryName = "testresult"

        val nodeA = Node(
            pathWithName = Path("de", "mw", "A"),
            language = SupportedLanguage.JAVA,
            physicalPath = "de/mw/File.java",
            nodeType = NodeType.CLASS,
            dependencies = setOf(
                Dependency.asWildcard("de", "mw"),
            ),
            usedTypes = setOf(
                Type.simple("B"),
            )
        )

        val nodeB = Node(
            pathWithName = Path("de", "mw", "B"),
            language = SupportedLanguage.JAVA,
            physicalPath = "de/mw/File.java",
            nodeType = NodeType.CLASS,
            dependencies = setOf(
                Dependency.asWildcard("de", "mw"),
            ),
            usedTypes = setOf(
                Type.simple("A")
            )
        )

        val fileReports = listOf(FileReport(listOf(nodeA, nodeB)))

        // when
        ProcessingPipeline.run(outputFileName, outputDirectoryName, fileReports, false)

        // then
        val result = getResult(Json { explicitNulls = false })

        val leaves = result.leaves
        assertTrue(leaves["de.mw.A"]!!.dependencies["de.mw.B"]!!.isCyclic)
        assertTrue(leaves["de.mw.B"]!!.dependencies["de.mw.A"]!!.isCyclic)
    }

    @Test
    fun `should resolve dependency from import to declare module export`() {
        // given
        val outputFileName = "test"
        val outputDirectoryName = "testresult"

        // Node from: declare module "MyModule" { export function myFunction(): void; }
        val declaredModuleNode = Node(
            pathWithName = Path("MyModule", "myFunction"),
            language = SupportedLanguage.TYPESCRIPT,
            physicalPath = "types.d.ts",
            nodeType = NodeType.FUNCTION,
            dependencies = emptySet(),
            usedTypes = emptySet()
        )

        // Node from: import { myFunction } from "MyModule"; export class Consumer { ... }
        val consumerNode = Node(
            pathWithName = Path("src", "consumer", "Consumer"),
            language = SupportedLanguage.TYPESCRIPT,
            physicalPath = "src/consumer.ts",
            nodeType = NodeType.CLASS,
            dependencies = setOf(
                Dependency(Path("MyModule", "myFunction"))
            ),
            usedTypes = setOf(Type.simple("myFunction"))
        )

        val fileReports = listOf(
            FileReport(listOf(declaredModuleNode)),
            FileReport(listOf(consumerNode))
        )

        // when
        ProcessingPipeline.run(outputFileName, outputDirectoryName, fileReports, false)

        // then
        val result = getResult(Json { explicitNulls = false })
        val leaves = result.leaves

        // The consumer should have a resolved dependency to the declared module
        val consumer = leaves["src.consumer.Consumer"]
            ?: throw AssertionError("Consumer node not found in leaves")
        assertThat(consumer.dependencies.keys).contains("MyModule.myFunction")
    }

    @Test
    fun `should resolve dependency when importing from declare module - end to end`() {
        // given
        val outputFileName = "test"
        val outputDirectoryName = "testresult"

        val declarationCode = """
            declare module "MyModule" {
                export function myFunction(): void;
            }
        """.trimIndent()

        val consumerCode = """
            import { myFunction } from "MyModule";

            export class Consumer {
                use() {
                    myFunction();
                }
            }
        """.trimIndent()

        // Analyze both files
        val declarationReport = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "types.d.ts",
                declarationCode
            )
        ).analyze()

        val consumerReport = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "src/consumer.ts",
                consumerCode
            )
        ).analyze()

        // when
        ProcessingPipeline.run(
            outputFileName,
            outputDirectoryName,
            listOf(declarationReport, consumerReport),
            false
        )

        // then
        val result = getResult(Json { explicitNulls = false })
        val leaves = result.leaves

        // Verify the declared module node exists
        val declaredModuleNode = leaves.keys.find { it.contains("myFunction") }
        assertThat(declaredModuleNode).isNotNull()

        // Verify the consumer exists and has a resolved dependency
        val consumer = leaves["src.consumer.Consumer"]
            ?: throw AssertionError("Consumer node not found. Available: ${leaves.keys}")

        // The dependency should be resolved (pointing to an existing node)
        val resolvedDependencies = consumer.dependencies.keys
        assertThat(resolvedDependencies)
            .withFailMessage(
                "Expected consumer to have dependency on declared module. " +
                    "Consumer deps: $resolvedDependencies, Available nodes: ${leaves.keys}"
            ).anyMatch { dep -> leaves.keys.any { node -> node == dep } }
    }

    @Test
    fun `should print visualization links after successful analysis`() {
        // Given
        val outputFileName = "test"
        val outputDirectoryName = "testresult"
        val nodeA = Node(
            pathWithName = Path("pkg", "A"),
            language = SupportedLanguage.JAVA,
            physicalPath = "pkg/A.java",
            nodeType = NodeType.CLASS,
            dependencies = emptySet(),
            usedTypes = emptySet()
        )
        val fileReports = listOf(FileReport(listOf(nodeA)))
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        Logger.setLevel(LogLevel.INFO)
        Logger.setLoggingDirectory(null)

        try {
            // When
            ProcessingPipeline.run(outputFileName, outputDirectoryName, fileReports, false)

            // Then
            val output = outputStream.toString()
            val expectedPath = File(outputDirectoryName, "$outputFileName.cg.json").absolutePath
            assertThat(output).contains(expectedPath)
            assertThat(output).contains("https://maibornwolff.github.io/DependaCharta/")
            assertThat(output).contains("https://github.com/MaibornWolff/DependaCharta#visualize-your-results")
        } finally {
            System.setOut(originalOut)
        }
    }
}
