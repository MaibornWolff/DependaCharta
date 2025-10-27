package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing.TypeDeclarationProcessor
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TypeDeclarationProcessorTest : BaseProcessorTest() {
    @Test
    fun `should extract type declarations correctly`() {
        // given
        val cppCode = """
class SubClassType : public SuperClass{};
union UnionType{};
enum EnumType{};
class ClassType {};
struct StructType{};
        """.trimIndent()

        val sut = TypeDeclarationProcessor()
        // when

        val result = process(sut, cppCode)
        // then
        assertThat(result.nodes).hasSize(5)
        assertThat(result.nodes.map { it.pathWithName.withDots() }).containsExactlyInAnyOrder(
            "MyNamespace.SubClassType",
            "MyNamespace.UnionType",
            "MyNamespace.EnumType",
            "MyNamespace.ClassType",
            "MyNamespace.StructType",
        )
    }

    @Test
    fun `should extract NodeType of struct nodes`() {
        // given
        val cppCode = """
             struct StructType{};
        """.trimIndent()
        val sut = TypeDeclarationProcessor()

        // when
        val result = process(sut, cppCode)

        // then
        assertThat(result.nodes).hasSize(1)
        assertThat(result.nodes[0].nodeType).isEqualTo(NodeType.VALUECLASS)
    }
}
