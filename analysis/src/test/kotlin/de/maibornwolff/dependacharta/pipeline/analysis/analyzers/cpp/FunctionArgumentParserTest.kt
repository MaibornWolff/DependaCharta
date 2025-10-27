package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FunctionArgumentParserTest {
    @Test
    fun `should parse simple argument into type`() {
        // given
        val code = "a"

        // when
        val result = FunctionArgumentParser.intoTypes(code)

        // then
        assertThat(result).containsExactly(Type.simple("a"))
    }

    @Test
    fun `should parse many arguments into types`() {
        // given
        val code = "a, b, c"

        // when
        val result = FunctionArgumentParser.intoTypes(code)

        // then
        val expected = listOf(Type.simple("a"), Type.simple("b"), Type.simple("c"))
        assertThat(result).containsAll(expected)
    }

    @Test
    fun `should parse type with type parameter into types`() {
        // given
        val code = "a<b>"

        // when
        val result = FunctionArgumentParser.intoTypes(code)

        // then
        val expected = listOf(Type.generic("a", listOf(Type.simple("b"))))
        assertThat(result).containsAll(expected)
    }

    @Test
    fun `should parse many types with type parameter into types`() {
        // given
        val code = "a<b>, c<d>, e<f>"

        // when
        val result = FunctionArgumentParser.intoTypes(code)

        // then
        val expected = listOf(
            Type.generic("a", listOf(Type.simple("b"))),
            Type.generic("c", listOf(Type.simple("d"))),
            Type.generic("e", listOf(Type.simple("f")))
        )
        assertThat(result).containsAll(expected)
    }

    @Test
    fun `should parse combination of types without and with type parameter into types`() {
        // given
        val code = "a<b>, c, e<f>, d"

        // when
        val result = FunctionArgumentParser.intoTypes(code)

        // then
        val expected = listOf(
            Type.generic("a", listOf(Type.simple("b"))),
            Type.simple("c"),
            Type.generic("e", listOf(Type.simple("f"))),
            Type.simple("d"),
        )
        assertThat(result).containsAll(expected)
    }

    @Test
    fun `should parse type with many type parameter into types`() {
        // given
        val code = "a<b, c>"

        // when
        val result = FunctionArgumentParser.intoTypes(code)

        // then
        val expected = listOf(
            Type.generic(
                "a",
                listOf(
                    Type.simple("b"),
                    Type.simple("c"),
                )
            ),
        )
        assertThat(result).containsAll(expected)
    }

    @Test
    fun `should parse many types with nested type parameter into types`() {
        // given
        val code = "a<b<c>>"

        // when
        val result = FunctionArgumentParser.intoTypes(code)

        // then
        val expected = listOf(
            Type.generic(
                "a",
                listOf(
                    Type.generic(
                        "b",
                        listOf(
                            Type.simple("c"),
                        )
                    ),
                )
            ),
        )
        assertThat(result).containsAll(expected)
    }

    @Test
    fun `should parse many types with nested type parameter and other types into types`() {
        // given
        val code = "a<b<c>, e>"

        // when
        val result = FunctionArgumentParser.intoTypes(code)

        // then
        val expected = listOf(
            Type.generic(
                "a",
                listOf(
                    Type.generic(
                        "b",
                        listOf(
                            Type.simple("c"),
                        )
                    ),
                    Type.simple("e"),
                )
            ),
        )
        assertThat(result).containsAll(expected)
    }

    @Test
    fun `should parse many types with nested type parameter and other types into types in different order`() {
        // given
        val code = "a<e, b<c>>"

        // when
        val result = FunctionArgumentParser.intoTypes(code)

        // then
        val expected = listOf(
            Type.generic(
                "a",
                listOf(
                    Type.simple("e"),
                    Type.generic(
                        "b",
                        listOf(
                            Type.simple("c"),
                        )
                    ),
                )
            ),
        )
        assertThat(result).containsAll(expected)
    }

    @Test
    fun `should omit part with namespace of name`() {
        // given
        val code = "a<x::y::e>"

        // when
        val result = FunctionArgumentParser.intoTypes(code)

        // then
        val expected = listOf(
            Type.generic(
                "a",
                listOf(
                    Type.simple("e"),
                )
            ),
        )
        assertThat(result).containsAll(expected)
    }

    @Test
    fun `should extract namespace parts as dependencies`() {
        // given
        val code = "x::y::a<o::p::r::b>"

        // when
        val result = FunctionArgumentParser.intoDependencies(code)

        // then
        val expected = setOf(
            Dependency.asWildcard("x", "y"),
            Dependency.asWildcard("o", "p", "r"),
        )
        assertThat(result).containsAll(expected)
    }

    @Test
    fun `should be empty when nothing is given`() {
        // given
        val code = ""

        // when
        val result = FunctionArgumentParser.intoTypes(code)

        // then
        val expected = emptyList<Type>()
        assertThat(result).containsAll(expected)
    }
}
