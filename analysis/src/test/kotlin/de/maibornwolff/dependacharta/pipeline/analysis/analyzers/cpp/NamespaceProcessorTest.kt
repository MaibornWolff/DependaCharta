package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing.NamespaceProcessor
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NamespaceProcessorTest : BaseProcessorTest() {
    @Test
    fun `should process namespace correctly`() {
        // given
        val cppCode = """
namespace Outer{
  class MyClass{};
  namespace Inner{
    class InnerClass{};
  }
}
        """.trimIndent()

        // when
        val sut = NamespaceProcessor()

        val result = processCompound(sut, cppCode)
        // then

        Assertions.assertThat(result.nodes[0].pathWithName.withDots()).isEqualTo("MyNamespace.Outer.MyClass")
        Assertions.assertThat(result.nodes[1].pathWithName.withDots()).isEqualTo("MyNamespace.Outer.Inner.InnerClass")
    }

    @Test
    fun `should process combined namespace correctly`() {
        // given
        val cppCode = """
namespace Foo::Outer{
  class MyClass{};
  }
        """.trimIndent()

        // when
        val sut = NamespaceProcessor()

        val result = processCompound(sut, cppCode)
        // then

        Assertions.assertThat(result.nodes[0].pathWithName.withDots()).isEqualTo("MyNamespace.Foo.Outer.MyClass")
    }

    @Test
    fun `should process empty namespace  name correctly`() {
        // given
        val cppCode = """
  namespace { class MyClass{}; }
        """.trimIndent()

        // when
        val sut = NamespaceProcessor()

        val result = processCompound(sut, cppCode)
        // then

        Assertions.assertThat(result.nodes[0].pathWithName.withDots()).isEqualTo("MyNamespace.MyClass")
    }

    @Test
    fun `should apply to namespace statements`() {
        val sut = NamespaceProcessor()

        // act & assert
        Assertions.assertThat(appliesTo(sut, "namespace Outer{}")).isTrue
        Assertions.assertThat(appliesTo(sut, "int a;")).isFalse
    }

    @Test
    fun `should recognize namespace as wildcard dependency`() {
        // given
        val cppCode = """
namespace Foo::Outer{
  class MyClass{};
  }
        """.trimIndent()
        val sut = NamespaceProcessor()

        // when
        val result = processCompound(sut, cppCode)

        // then
        assertThat(result.nodes[0].dependencies)
            .contains(
                Dependency(Path("MyNamespace", "Foo", "Outer"), isWildcard = true),
            )
    }
}
