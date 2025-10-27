package de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.model.toImport
import de.maibornwolff.codegraph.pipeline.analysis.model.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PathUtilsTest {
    @Test
    fun `should resolve relative import on same directory`() {
        // given
        val import = "./MyImport".toImport()
        val relativePath = Path("test/utils/PathUtilsTest.kt".split('/'))

        // when
        val resolvedPath = resolveRelativePath(import, relativePath)

        // then
        val expected = listOf("test", "utils", "MyImport")
        assertThat(resolvedPath.parts)
            .containsExactlyElementsOf(expected)
    }

    @Test
    fun `should resolve relative import on nested directory`() {
        // given
        val import = "../MyImport".toImport()
        val relativePath = Path("test/utils/PathUtilsTest.kt".split('/'))

        // when
        val resolvedPath = resolveRelativePath(import, relativePath)

        // then
        val expected = listOf("test", "MyImport")
        assertThat(resolvedPath.parts)
            .containsExactlyElementsOf(expected)
    }
}
