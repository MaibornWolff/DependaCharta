package de.maibornwolff.dependacharta.pipeline.analysis.sychronization

import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.synchronization.RootDirectoryWalker
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class RootDirectoryWalkerTest {
    @Test
    fun `Should only return files with allowed ending`() {
        // given
        val testee =
            RootDirectoryWalker(File("src/test/resources/rootdirectorywalker"), listOf(SupportedLanguage.JAVA, SupportedLanguage.C_SHARP))

        // when
        val files = testee.walk()

        // then
        val matchingFiles = files.toSet()
        assertThat(matchingFiles).hasSize(8)
        matchingFiles.forEach { file ->
            assertThat(file).containsAnyOf(".java", ".cs")
        }
    }

    @Test
    fun `Should ignore file in ignoredDirectory (node_modules)`() {
        // given
        val testee =
            RootDirectoryWalker(File("src/test/resources/rootdirectorywalker/ignoreddirectories"), listOf(SupportedLanguage.TYPESCRIPT))

        // when
        val files = testee.walk()

        // then
        assertThat(files.toList()).hasSize(1)
    }

    @Test
    fun `Should ignore file with ignoredFileEnding`() {
        // given
        val testee =
            RootDirectoryWalker(File("src/test/resources/rootdirectorywalker/ignoredfileendings"), listOf(SupportedLanguage.TYPESCRIPT))

        // when
        val files = testee.walk()

        // then
        assertThat(files.toList()).isEmpty()
    }

    @Test
    fun `Should return fileInfo`() {
        // given
        val expectedFileName = "Sample.java"
        val testee = RootDirectoryWalker(File("src/test/resources/rootdirectorywalker"), listOf(SupportedLanguage.JAVA))
        val filePath = testee.walk().toList().find { it.endsWith("Sample.java") }!!

        // when
        val fileInfo = testee.getFileInfo(filePath)

        // then
        val expected = FileInfo(
            language = SupportedLanguage.JAVA,
            physicalPath = expectedFileName,
            content = "java test",
            analysisRoot = File("src/test/resources/rootdirectorywalker")
        )
        assertThat(fileInfo).isEqualTo(expected)
    }

    @Test
    fun `Should filter UTF-8 BOM character out of fileInfo content`() {
        // given
        val testee = RootDirectoryWalker(File("src/test/resources/rootdirectorywalker"), listOf(SupportedLanguage.C_SHARP))
        val filePath = testee.walk().toList()[0]

        // when
        val fileInfo = testee.getFileInfo(filePath)

        // then
        assertThat(fileInfo.content).isEqualTo("CSharp test.")
    }

    @Test
    fun `Should ignore Go test files ending with _test dot go`() {
        // given
        val testee = RootDirectoryWalker(File("src/test/resources/rootdirectorywalker/go-test-files"), listOf(SupportedLanguage.GO))

        // when
        val files = testee.walk()

        // then
        val fileList = files.toList()
        assertThat(fileList).hasSize(1)
        assertThat(fileList.first()).endsWith("main.go")
        assertThat(fileList.first()).doesNotContain("_test.go")

        // Verify that main_test.go is indeed filtered out
        assertThat(fileList.none { it.contains("main_test.go") }).isTrue()
    }

    @Test
    fun `Should ignore Java test files ending with Test dot java`() {
        // given
        val testee = RootDirectoryWalker(File("src/test/resources/rootdirectorywalker/java-test-files"), listOf(SupportedLanguage.JAVA))

        // when
        val files = testee.walk()

        // then
        val fileList = files.toList()
        assertThat(fileList).hasSize(1)
        assertThat(fileList.first()).endsWith("Main.java")
        assertThat(fileList.none { it.contains("MainTest.java") }).isTrue()
    }

    @Test
    fun `Should ignore Kotlin test files ending with Test dot kt`() {
        // given
        val testee = RootDirectoryWalker(File("src/test/resources/rootdirectorywalker/kotlin-test-files"), listOf(SupportedLanguage.KOTLIN))

        // when
        val files = testee.walk()

        // then
        val fileList = files.toList()
        assertThat(fileList).hasSize(1)
        assertThat(fileList.first()).endsWith("Main.kt")
        assertThat(fileList.none { it.contains("MainTest.kt") }).isTrue()
    }

    @Test
    fun `Should ignore files in test directory`() {
        // given
        val testee = RootDirectoryWalker(File("src/test/resources/rootdirectorywalker/test-directory"), listOf(SupportedLanguage.JAVA))

        // when
        val files = testee.walk()

        // then
        assertThat(files.toList()).isEmpty()
    }

    @Test
    fun `Should ignore files in tests directory`() {
        // given
        val testee = RootDirectoryWalker(File("src/test/resources/rootdirectorywalker/tests-directory"), listOf(SupportedLanguage.JAVA))

        // when
        val files = testee.walk()

        // then
        assertThat(files.toList()).isEmpty()
    }

    @Test
    fun `Should ignore files in __tests__ directory`() {
        // given
        val testee = RootDirectoryWalker(File("src/test/resources/rootdirectorywalker/jest-tests"), listOf(SupportedLanguage.TYPESCRIPT))

        // when
        val files = testee.walk()

        // then
        assertThat(files.toList()).isEmpty()
    }

    // --- File size filtering tests ---

    @Test
    fun `Should skip files larger than maxFileSizeKB`() {
        // given
        val testee = RootDirectoryWalker(
            File("src/test/resources/rootdirectorywalker/file-size-filtering"),
            listOf(SupportedLanguage.JAVA),
            maxFileSizeKB = 1
        )

        // when
        val files = testee.walk().toList()

        // then
        assertThat(files).hasSize(1)
        assertThat(files.first()).endsWith("small-file.java")
    }

    @Test
    fun `Should include files below maxFileSizeKB`() {
        // given
        val testee = RootDirectoryWalker(
            File("src/test/resources/rootdirectorywalker/file-size-filtering"),
            listOf(SupportedLanguage.JAVA),
            maxFileSizeKB = 2
        )

        // when
        val files = testee.walk().toList()

        // then
        assertThat(files).hasSize(2)
    }

    @Test
    fun `Should not filter by size when maxFileSizeKB is zero`() {
        // given
        val testee = RootDirectoryWalker(
            File("src/test/resources/rootdirectorywalker/file-size-filtering"),
            listOf(SupportedLanguage.JAVA),
            maxFileSizeKB = 0
        )

        // when
        val files = testee.walk().toList()

        // then
        assertThat(files).hasSize(2)
    }

    // --- Minified file exclusion tests ---

    @Test
    fun `Should exclude min js files by default`() {
        // given
        val testee = RootDirectoryWalker(
            File("src/test/resources/rootdirectorywalker/minified-files"),
            listOf(SupportedLanguage.TYPESCRIPT, SupportedLanguage.JAVASCRIPT)
        )

        // when
        val files = testee.walk().toList()

        // then
        assertThat(files).hasSize(2)
        assertThat(files).anyMatch { it.endsWith("app.ts") }
        assertThat(files).anyMatch { it.endsWith("app.js") }
        assertThat(files).noneMatch { it.contains(".min.") }
    }

    // --- Custom exclusion tests ---

    @Test
    fun `Should exclude additional directories from excludedDirs`() {
        // given
        val testee = RootDirectoryWalker(
            File("src/test/resources/rootdirectorywalker/custom-exclusions"),
            listOf(SupportedLanguage.JAVA),
            excludedDirs = listOf("generated", "vendor")
        )

        // when
        val files = testee.walk().toList()

        // then
        assertThat(files).hasSize(1)
        assertThat(files.first()).endsWith("App.java")
    }

    @Test
    fun `Should exclude additional suffixes from excludedSuffixes`() {
        // given
        val testee = RootDirectoryWalker(
            File("src/test/resources/rootdirectorywalker/minified-files"),
            listOf(SupportedLanguage.TYPESCRIPT, SupportedLanguage.JAVASCRIPT),
            excludedSuffixes = listOf(".min.ts", ".min.js")
        )

        // when
        val files = testee.walk().toList()

        // then
        assertThat(files).hasSize(2)
        assertThat(files).noneMatch { it.contains(".min.") }
    }

    @Test
    fun `Should clear all defaults when useDefaultExcludes is false`() {
        // given
        val testee = RootDirectoryWalker(
            File("src/test/resources/rootdirectorywalker/ignoreddirectories"),
            listOf(SupportedLanguage.TYPESCRIPT),
            useDefaultExcludes = false
        )

        // when
        val files = testee.walk().toList()

        // then - should include files in node_modules since defaults are disabled
        assertThat(files).hasSize(2)
    }
}
