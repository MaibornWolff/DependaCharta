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
        assertThat(matchingFiles).hasSize(2)
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
        val expectedFileName = "Test.java"
        val testee = RootDirectoryWalker(File("src/test/resources/rootdirectorywalker"), listOf(SupportedLanguage.JAVA))
        val filePath = testee.walk().toList()[0]

        // when
        val fileInfo = testee.getFileInfo(filePath)

        // then
        val expected = FileInfo(
            language = SupportedLanguage.JAVA,
            physicalPath = expectedFileName,
            content = "java test"
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
}
