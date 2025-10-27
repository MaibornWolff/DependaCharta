package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing.AliasProcessor
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class AliasProcessorTest : BaseProcessorTest() {
    @Test
    fun `should extract alias correctly`() {
        // given
        val cppCode = """
using new_existing_type = existing_type;
using new_List_Foo = List<Foo>;
using new_FooBar = foo::bar::Foobar;
        """.trimIndent()

        // when
        val sut = AliasProcessor()

        val result = process(sut, cppCode)
        // then

        val t0 = Type.simple("existing_type")
        val t1 = Type.generic("List", listOf(Type.simple("Foo")))
        val t2 = Type.simple("Foobar")

        Assertions.assertThat(result.context.usedTypes).containsExactlyInAnyOrder(t0, t1, t2)
    }

    @Test
    fun `should apply to using statements`() {
        val sut = AliasProcessor()

        // act & assert
        Assertions.assertThat(appliesTo(sut, "typedef existing_type new_existing_type;")).isFalse
        Assertions.assertThat(appliesTo(sut, "using new_existing_type = existing_type;")).isTrue
        Assertions.assertThat(appliesTo(sut, "int a;")).isFalse
    }
}
