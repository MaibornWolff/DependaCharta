package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing.DefineProcessor
import de.maibornwolff.codegraph.pipeline.analysis.model.Type
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class DefineProcessorTest : BaseProcessorTest() {
    @Test
    fun `should extract usings correctly`() {
        // given
        val cppCode = """
            #ifdef foo
              int foo;
            #else
              string bar
            #endif
        """.trimIndent()

        // when
        val sut = DefineProcessor()

        val result = processCompound(sut, cppCode)
        // then
        Assertions
            .assertThat(result.context.usedTypes)
            .containsExactly(Type.simple("int"), Type.simple("string"))
    }

    @Test
    fun `should apply to using statements`() {
        val sut = DefineProcessor()

        // act & assert
        Assertions.assertThat(appliesTo(sut, "#ifdef foo")).isTrue
        Assertions.assertThat(appliesTo(sut, "int a;")).isFalse
    }
}