package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing.TypeDefProcessor
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TypeDefProcessorTest : BaseProcessorTest() {
    @Test
    fun `should extract typedef correctly`() {
        // given
        val cppCode = """
typedef existing_type new_existing_type;
typedef List<Foo> new_List_Foo;
typedef foo::bar::Foobar new_FooBar;
        """.trimIndent()

        // when
        val sut = TypeDefProcessor()

        val result = process(sut, cppCode)
        // then

        val t0 = Type.simple("existing_type")
        val t1 = Type.generic("List", listOf(Type.simple("Foo")))
        val t2 = Type.simple("Foobar")
        val d1 = Dependency.asWildcard("foo", "bar")

        assertThat(result.context.usedTypes).containsExactlyInAnyOrder(t0, t1, t2)
        assertThat(result.context.getDependencies()).containsAll(listOf(d1))
    }

    @Test
    fun `should apply to typedef statements`() {
        val sut = TypeDefProcessor()

        // act & assert
        Assertions.assertThat(appliesTo(sut, "typedef existing_type new_existing_type;")).isTrue
        Assertions.assertThat(appliesTo(sut, "using new_existing_type = existing_type;")).isFalse
        Assertions.assertThat(appliesTo(sut, "int a;")).isFalse
    }
}
