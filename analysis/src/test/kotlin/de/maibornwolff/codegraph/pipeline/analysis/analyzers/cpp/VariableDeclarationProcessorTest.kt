package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing.VariableDeclarationProcessor
import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency
import de.maibornwolff.codegraph.pipeline.analysis.model.Type
import de.maibornwolff.codegraph.pipeline.analysis.model.TypeOfUsage
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class VariableDeclarationProcessorTest : BaseProcessorTest() {
    //   @Test

    val genericType: Type = Type.generic("a", listOf(Type.simple("b")))

    @Test
    fun `should extract usedtype of variable declarations correctly`() {
        testDeclaration("int foo;")
        testDeclaration("a foo;", "a")
        testDeclaration("a::b foo;", "b")
        testDeclaration("a<b> foo;", genericType)
    }

    @Test
    fun `should extract usedtype of field declarations correctly`() {
        testDeclaration("class x{private: int foo;};")
    }

    @Test
    fun `should extract usedtype of parameter declaration correctly`() {
        testDeclaration("void method(int param)")
        testDeclaration("void method(a param)", "a")
        testDeclaration("void method(a::b param)", "b")
    }

    @Test
    fun `should extract types used in generic type`() {
        testDeclaration("void method(a<b> param)", genericType)
        testDeclaration("void method(a<b,c> param)", Type.generic("a", listOf(Type.simple("b"), Type.simple("c"))))
        testDeclaration(
            "Foo::Foo(std::shared_ptr<Bar<A,B>> a){}",
            Type.generic("shared_ptr", listOf(Type.generic("Bar", listOf(Type.simple("A"), Type.simple("B"))))),
            Dependency.asWildcard("std"),
        )
        testDeclaration(
            "Foo::Foo(std::shared_ptr<Bar<A,NoStd::B<C>>> a){}",
            Type.generic(
                "shared_ptr",
                listOf(Type.generic("Bar", listOf(Type.simple("A"), Type.generic("B", listOf(Type.simple("C"))))))
            ),
            Dependency.asWildcard("std"),
            Dependency.asWildcard("NoStd"),
        )
    }

    @Test
    fun `should extract type when last`() { // TODO: test method parameter?
        // given
        val cppCode = "F(a<b<c>,e>){}"
        val sut = VariableDeclarationProcessor()
        val expected = listOf(
            Type.generic(
                "a",
                listOf(
                    Type.generic("b", listOf(Type.simple("c"))),
                    Type.simple("e")
                )
            )
        )

        // when
        val result = processCompound(sut, cppCode)

        // then
        Assertions.assertThat(result.context.usedTypes).containsAll(expected)
    }

    @Test
    fun `should be green`() { // TODO: test method parameter?
        // given
        val cppCode = "F(a<e,b<c>>){}"
        val sut = VariableDeclarationProcessor()
        val expected = listOf(
            Type.generic(
                "a",
                listOf(
                    Type.simple("e"),
                    Type.generic("b", listOf(Type.simple("c")))
                )
            )
        )

        // when
        val result = processCompound(sut, cppCode)

        // then
        Assertions.assertThat(result.context.usedTypes).containsAll(expected)
    }

    @Test
    fun `should extract usedtype of template argument list correctly`() {
        testDeclaration("method<int>();")
        testDeclaration("method<a>();", "a")
        testDeclaration("method<a::b>();", "b")
        testDeclaration("method<a<b>>();", genericType)
    }

    @Test
    fun `should extract usedtype of cast operation correctly`() {
        testDeclaration("(int)x;")
        testDeclaration("(a)x;", "a")
        testDeclaration("(a::b)x;", "b")
        testDeclaration("(a<b>)x;", genericType)
    }

    @Test
    fun `should extract scoped type of call (potential constructor)`() {
        testDeclaration("foo::int();", Type.simple("int"), Dependency.asWildcard("foo"))
    }

    @Test
    fun `should extract type of throwed exception correctly`() {
        testDeclaration("throw a();", "a")
        testDeclaration("throw a::b();", "b")
        testDeclaration("throw a<b>();", genericType)
    }

    private fun testDeclaration(
        cppCode: String,
        expectedType: String = "int"
    ) {
        testDeclaration(cppCode, Type.simple(expectedType, TypeOfUsage.USAGE))
    }

    private fun testDeclaration(
        cppCode: String,
        expectedType: Type,
        vararg expectedDependencies: Dependency
    ) {
        val sut = VariableDeclarationProcessor()
        // when
        val result = processCompound(sut, cppCode)
        // then
        Assertions.assertThat(result.context.usedTypes).contains(expectedType)
        Assertions.assertThat(result.context.getDependencies()).containsAll(expectedDependencies.toList())
    }
}
