package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing.GenericTemplateProcessor
import de.maibornwolff.codegraph.pipeline.analysis.model.Type
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class GenericTemplateProcessorTest : BaseProcessorTest() {
    @Test
    fun `should extract template constraints correctly`() {
        // given
        val cppCode = """
template <typename T>
requires std::integral<T> && std::floating_point<T> 
class MyClass {};
        """.trimIndent()

        // when
        val sut = GenericTemplateProcessor()

        val result = processCompound(sut, cppCode)
        // then

        Assertions.assertThat(result.nodes[0].usedTypes).containsAll(
            listOf(
                Type.generic("integral", listOf(Type.simple("T"))),
                Type.generic("floating_point", listOf(Type.simple("T")))
            )
        )
    }

    @Test
    fun `should extract class used types correctly`() {
        // given
        val cppCode = """
template <typename T>
class MyClass {
  string foo;
};
        """.trimIndent()

        // when
        val sut = GenericTemplateProcessor()

        val result = processCompound(sut, cppCode)
        // then

        Assertions.assertThat(result.nodes[0].usedTypes).containsAll(listOf(Type.simple("string")))
    }

    @Test
    fun `should apply to using statements`() {
        val sut = GenericTemplateProcessor()

        // act & assert
        Assertions.assertThat(appliesTo(sut, "template <typename T> class MyClass { };")).isTrue
        Assertions.assertThat(appliesTo(sut, "int a;")).isFalse
    }
}
