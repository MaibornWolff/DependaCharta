package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.BaseProcessorTest
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VariableDeclarationProcessorTest : BaseProcessorTest() {
    @Test
    fun `should extract scoped type even if global`() {
        // given
        val cppCode = """
return ::a::b();
        """.trimIndent()

        // when
        val sut = VariableDeclarationProcessor()

        val result = processCompound(sut, cppCode)

        // then
        assertThat { result.context.usedTypes.containsAll(listOf(Type.simple("a.b"))) }
    }
}
