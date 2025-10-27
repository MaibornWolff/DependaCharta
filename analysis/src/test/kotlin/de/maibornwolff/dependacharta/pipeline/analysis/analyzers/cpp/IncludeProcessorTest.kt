package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.model.toImport
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing.IncludeProcessor
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IncludeProcessorTest : BaseProcessorTest() {
    @Test
    fun `should extract all includes`() {
        // given
        val cppCode = """
#include "File.h"
#include <Bar>
        """.trimIndent()

        // when
        val sut = IncludeProcessor()

        val result = process(sut, cppCode)
        // then

        val includes = result.context.includes.map { it.includeFile }
        assertThat(includes).containsAll(listOf("Bar", "File.h").map { it.toImport() })
    }

    @Test
    fun `should apply to using statements`() {
        val sut = IncludeProcessor()

        // act & assert
        Assertions.assertThat(appliesTo(sut, "#include <Bar>")).isTrue
        Assertions.assertThat(appliesTo(sut, "int a;")).isFalse
    }

    @Test
    fun `should add includes only once`() {
        // given
        val cppCode = """#include "File.h"
#include "File.h"
#include <Bar>
        """.trimIndent()

        // when
        val sut = IncludeProcessor()
        val result = process(sut, cppCode)

        // then
        val includes = result.context.includes.map { it.includeFile }
        Assertions.assertThat(includes).containsExactlyInAnyOrder("File.h".toImport(), "Bar".toImport())
    }
}
