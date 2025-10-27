package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing.InheritanceProcessor
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InheritanceProcessorTest : BaseProcessorTest() {
    @Test
    fun `should extract type declarations correctly`() {
        // given
        val cppCode = """
class SubClassType : public SuperClass{};
class SubStruct : public SuperStruct, AnotherSuperStruct{};
struct SubStruct1 : private SuperStruct1{};
        """.trimIndent()

        val sut = InheritanceProcessor()
        // when

        val result = process(sut, cppCode)
        // then
        Assertions
            .assertThat(result.context.usedTypes.map { it.name })
            .containsExactlyInAnyOrder("SuperClass", "SuperStruct", "SuperStruct1", "AnotherSuperStruct")
    }

    @Test
    fun `should extract generic type declarations correctly`() {
        // given
        val cppCode = """
class SubClassType : public SuperClass<A,B>{};
        """.trimIndent()

        val sut = InheritanceProcessor()
        // when

        val result = process(sut, cppCode)
        // then
        val expectedUsedType = Type.generic("SuperClass", listOf(Type.simple("A"), Type.simple("B")))
        Assertions.assertThat(result.context.usedTypes).containsAll(listOf(expectedUsedType))
    }

    @Test
    fun `should extract namespace of base class as dependency`() {
        // given
        val cppCode = """
            class SubClassType : public A::B::SuperClass{};
        """.trimIndent()

        val sut = InheritanceProcessor()

        // when
        val result = process(sut, cppCode)

        // then
        assertThat(result.context.getDependencies()).contains(Dependency(path = Path("A", "B"), isWildcard = true))
    }

    @Test
    fun `should apply to union, enum, class and struct statements`() {
        val sut = InheritanceProcessor()

        Assertions.assertThat(appliesTo(sut, "class SubClassType : public SuperClass{}")).isTrue
        Assertions.assertThat(appliesTo(sut, "struct SubClassType {}")).isTrue
        Assertions.assertThat(appliesTo(sut, "enum Sample{};")).isFalse
        Assertions.assertThat(appliesTo(sut, "int a;")).isFalse
    }
}
