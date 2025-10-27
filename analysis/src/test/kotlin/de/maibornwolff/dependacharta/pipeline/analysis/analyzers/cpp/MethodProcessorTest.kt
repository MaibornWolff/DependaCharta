package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing.MethodProcessor
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class
MethodProcessorTest : BaseProcessorTest() {
    @Test
    fun `should extract used types from method correctly`() {
        // given
        val cppCode = """
Simple method(ParamType param){
      LocalType foo;
      List<unsigned int> bla;
      foo::bar::MyClass xyzzy;
      
      int variable = Factor<TemplateType>();
      if (BarFoo<TemplateType1>()) {}
      }
        """.trimIndent()

        // when
        val sut = MethodProcessor()

        val result = process(sut, cppCode)
        // then

        Assertions.assertThat(result.context.usedTypes).containsAll(
            listOf(
                Type.simple("Simple"),
                Type.simple("ParamType"),
                Type.simple("LocalType"),
                Type.generic("List", listOf(Type.simple("int"))),
                Type.simple("MyClass"),
                Type.simple("TemplateType"),
                Type.simple("TemplateType1"),
                Type.simple("int"),
            )
        )
    }

    @Test
    fun `should extract used generic types from constructor correctly`() {
        // given
        val cppCode = """
PersistedCreatures::PersistedCreatures(std::shared_ptr<CreatureRepository<a::b::c::CreatureEntity, std::string>> repository)
    : repository(repository) {}
        """.trimIndent()

        // when
        val sut = MethodProcessor()

        val result = process(sut, cppCode)
        // then

        Assertions.assertThat(result.context.usedTypes).containsAll(
            listOf(
                Type.generic(
                    "shared_ptr",
                    listOf(Type.generic("CreatureRepository", listOf(Type.simple("CreatureEntity"), Type.simple("string"))))
                ),
            )
        )
        assertThat(result.context.getDependencies())
            .containsAll(
                setOf(
                    Dependency(path = Path("std"), isWildcard = true),
                    Dependency(path = Path("a", "b", "c"), isWildcard = true)
                )
            )
    }

    @Test
    fun `should extract namespace and types used in constructor and initializer`() {
        // given
        val cppCode = """
ArmorClass::ArmorClass(int base, int bonus)
    : ArmorClass(base, bonus, de::sots::cellarsandcentaurs::application::CreatureUtil::STANDARD_ARMOR_CLASS_DESCRIPTION)
{}
        """.trimIndent()

        // when
        val sut = MethodProcessor()

        val result = process(sut, cppCode)
        // then

        Assertions.assertThat(result.context.usedTypes).containsAll(
            listOf(
                Type.simple("int"),
                Type.simple("CreatureUtil"),
            )
        )
        assertThat(result.context.getDependencies())
            .containsAll(
                setOf(
                    Dependency(path = Path("de", "sots", "cellarsandcentaurs", "application"), isWildcard = true)
                )
            )
    }

    @Test
    fun `should extract dependencies from field initializers correctly`() {
        // given
        val cppCode = """
Foo(int repository)
    : value(foo::bar::Value()),
      value(bar::foo::value){}
        """.trimIndent()

        // when
        val sut = MethodProcessor()

        val result = process(sut, cppCode)
        // then

        Assertions.assertThat(result.context.usedTypes).containsAll(
            listOf(
                Type.simple("int"),
                Type.simple("bar"),
                Type.simple("foo")
            )
        )
    }

    @Test
    fun `should extract method return types from method correctly`() {
        assertExtractedReturnType("std::List<Foo> Foo::Bar() {}", Type.generic("List", listOf(Type.simple("Foo"))))
        assertExtractedReturnType("S1 Foo::Bar() {}", "S1")
        assertExtractedReturnType("S1** Bar(){}", "S1")
        assertExtractedReturnType("S1 Bar(){}", "S1")
        assertExtractedReturnType("S1& Bar(){}", "S1")
        assertExtractedReturnType("S1& operator=(){}", "S1")
        assertNoReturnType("Bar() {}")
        assertNoReturnType("~Bar() {}")
        assertNoReturnType("~Bar() {}")
    }

    fun assertExtractedReturnType(
        code: String,
        expectedType: String
    ) = assertExtractedReturnType(code, Type.simple(expectedType))

    fun assertNoReturnType(code: String) {
        val sut = MethodProcessor()
        val result = process(sut, code)
        Assertions.assertThat(result.context.usedTypes).isEmpty()
    }

    fun assertExtractedReturnType(
        code: String,
        expectedType: Type
    ) {
        val sut = MethodProcessor()
        val result = process(sut, code)
        Assertions.assertThat(result.context.usedTypes).containsExactly(expectedType)
    }

    @Test
    fun `should find correct names of potential nodes`() {
        // given
        val code = """
            X::Y A::B::C::Foo() {
                return "something";
            }
        """.trimIndent()
        val sut = MethodProcessor()

        // when
        val result = process(sut, code)

        // then
        assertThat(result.nodes).hasSize(1)
        assertThat(result.nodes.first().name()).isEqualTo("C")
        assertThat(
            result.nodes
                .first()
                .pathWithName
                .withoutName()
        ).containsExactly("A", "B")
    }

    @Test
    fun `should apply to method declarations`() {
        val sut = MethodProcessor()

        // act & assert
        Assertions.assertThat(appliesTo(sut, "Simple method(ParamType param){}")).isTrue
        Assertions.assertThat(appliesTo(sut, "int a;")).isFalse
    }
}
