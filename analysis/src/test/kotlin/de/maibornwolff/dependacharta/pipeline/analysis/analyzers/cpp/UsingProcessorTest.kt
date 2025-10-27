package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing.UsingProcessor
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UsingProcessorTest : BaseProcessorTest() {
    @Test
    fun `should extract using directive for whole namespace correctly`() {
        // given
        val cppCode = """
using namespace Foo::Bar;
        """.trimIndent()

        // when
        val sut = UsingProcessor()

        val result = processCompound(sut, cppCode)
        // then

        assertThat(result.context.getDependencies().toList()).containsExactly(
            Dependency.asWildcard("Foo", "Bar"),
        )
    }

    @Test
    fun `should apply to using statements`() {
        val sut = UsingProcessor()

        // act & assert
        assertThat(appliesTo(sut, "using ab;")).isTrue
        assertThat(appliesTo(sut, "int a;")).isFalse
    }

    @Test
    fun `should ignore inherited constructor, because type and usage is extracted with inheritance processor`() {
        // given
        val cppCode = """
            class Foo : Bar {
                using Bar::Bar;
            };
        """.trimIndent()

        // when
        val sut = UsingProcessor()

        val result = processCompound(sut, cppCode)
        // then

        assertThat(result.context.getDependencies()).isEmpty()
    }

    @Test
    fun `should extract using enum declaration with namespace correctly`() {
        // given
        val cppCode = """
            using enum foo::EnumBar;
        """.trimIndent()

        // when
        val sut = UsingProcessor()

        val result = processCompound(sut, cppCode)
        // then

        assertThat(result.context.getDependencies().toList()).containsExactly(
            Dependency.simple("foo", "EnumBar"),
        )
    }

    @Test
    fun `should extract using enum declaration as type correctly`() {
        // given
        val cppCode = """
            using enum EnumFoo;
        """.trimIndent()

        // when
        val sut = UsingProcessor()

        val result = processCompound(sut, cppCode)
        // then
        assertThat(result.context.usedTypes.toList()).containsExactly(
            Type.simple("EnumFoo"),
        )
    }

    @Test
    fun `should extract using declarations for types and methods correctly`() {
        // given
        val cppCode = """
            using Foo::Bar;
        """.trimIndent()

        // when
        val sut = UsingProcessor()

        val result = processCompound(sut, cppCode)
        // then

        assertThat(result.context.getDependencies().toList()).containsExactly(
            Dependency.simple("Foo", "Bar"),
        )
    }
}
