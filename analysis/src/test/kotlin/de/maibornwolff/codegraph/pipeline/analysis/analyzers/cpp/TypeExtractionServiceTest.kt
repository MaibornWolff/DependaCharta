package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp

import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency
import de.maibornwolff.codegraph.pipeline.analysis.model.Type
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterCpp

class TypeExtractionServiceTest {
    private var cpp = TreeSitterCpp()

    @Test
    fun `should extract simple types`() {
        shouldExtract("int Foo", Type.simple("int"))
        shouldExtract("Bar Foo", Type.simple("Bar"))
    }

    @Test
    fun `should ignore size type specifier`() {
        shouldExtract("unsigned int Foo;", Type.simple("int"))
        shouldExtract("signed double Foo;", Type.simple("double"))
    }

    @Test
    fun `should extract generic types`() {
        shouldExtract(
            "List<a,b,c> int Foo;",
            Type.generic("List", listOf(Type.simple("a"), Type.simple("b"), Type.simple("c")))
        )
    }

    @Test
    fun `should extract scoped generic types`() {
        shouldExtract(
            "foo::bar::List<a::b<c>> foo",
            Type.generic("List", listOf(Type.generic("b", listOf(Type.simple("c"))))),
            Dependency.asWildcard("foo", "bar"),
            Dependency.asWildcard("a")
        )
        shouldExtract(
            "foo::List<a,b,c> Foo;",
            Type.generic("List", listOf(Type.simple("a"), Type.simple("b"), Type.simple("c"))),
            Dependency.asWildcard("foo")
        )
    }

    private fun shouldExtract(
        cppCode: String,
        expectedType: Type,
        vararg expectedDependencies: Dependency
    ) {
        val node = parseCode(cppCode, cpp)
        val (extractedTypes, dependencies) = node.extractTypeWithFoundNamespacesAsDependencies(cppCode)
        Assertions.assertThat(extractedTypes.first()).isEqualTo(expectedType)
        Assertions.assertThat(dependencies).containsAll(expectedDependencies.toList())
    }

    private fun parseCode(
        cppCode: String,
        cpp: TreeSitterCpp
    ): TSNode {
        val parser = TSParser()
        parser.language = cpp
        val tree = parser.parseString(null, cppCode)
        return tree.rootNode.getChild(0).getChildByFieldName("type")
    }
}
