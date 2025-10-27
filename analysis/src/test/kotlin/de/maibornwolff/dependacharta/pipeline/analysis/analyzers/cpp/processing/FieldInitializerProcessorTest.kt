package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.BaseProcessorTest
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.analysis.model.TypeOfUsage
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class FieldInitializerProcessorTest : BaseProcessorTest() {
    @Test
    fun `should extract used namespaces and types from field initializer list`() {
        // given
        val cppCode = """
            Foo::Bar()
    : type_{A::B::GLOBAL_CONSTANT} {}
    }
        """.trimIndent()

        val sut = FieldInitializerProcessor()

        // when
        val result = process(sut, cppCode)

        // then
        val actualDependencies = result.context.getDependencies()
        val actualTypes = result.context.usedTypes
        assertThat(actualDependencies).containsAll(
            setOf(
                Dependency.asWildcard("A"),
            )
        )
        assertThat(actualTypes).containsAll(
            setOf(
                Type.simple("B", typeOfUsage = TypeOfUsage.INSTANTIATION),
            )
        )
    }

    @Test
    fun `should extract type only from field initializer list`() {
        // given
        val cppCode = """
            Foo::Bar()
    : type_{A::GLOBAL_CONSTANT} {}
    }
        """.trimIndent()

        val sut = FieldInitializerProcessor()

        // when
        val result = process(sut, cppCode)

        // then
        val actualDependencies = result.context.getDependencies()
        val actualTypes = result.context.usedTypes
        assertThat(actualDependencies).isEmpty()
        assertThat(actualTypes).containsAll(
            setOf(
                Type.simple("A", typeOfUsage = TypeOfUsage.INSTANTIATION),
            )
        )
    }

    @Test
    fun `should extract namespaces and types from function call in initializer list`() {
        // given
        val cppCode = """
            FooClass::FooClass(int base, int bonus)
    : BarClass(base, bonus, A::B::C::GLOBAL_CONSTANT)
{}
        """.trimIndent()

        val sut = FieldInitializerProcessor()

        // when
        val result = process(sut, cppCode)

        // then
        val actualDependencies = result.context.getDependencies()
        val actualTypes = result.context.usedTypes
        assertThat(actualDependencies).containsAll(
            setOf(
                Dependency.asWildcard("A", "B"),
            )
        )
        assertThat(actualTypes).containsAll(
            setOf(
                Type.simple("C", typeOfUsage = TypeOfUsage.INSTANTIATION),
            )
        )
    }

    @Test
    fun `should not return unparsable types`() {
        // given
        val cppCode = """
            FooClass::FooClass(int base, int bonus)
    : BarClass(base, bonus)
{}
        """.trimIndent()

        val sut = FieldInitializerProcessor()

        // when
        val result = process(sut, cppCode)

        // then
        val actualTypes = result.context.usedTypes
        assertThat(actualTypes).isEmpty()
    }
}
