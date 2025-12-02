package de.maibornwolff.dependacharta.pipeline.analysis.analyzers

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.CSharpAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.java.JavaAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin.KotlinAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.TypescriptAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class LanguageAnalyzerFactoryTest {
    companion object {
        @JvmStatic
        fun getFileInfoToAnalyzer(): List<Arguments> {
            val javaFileInfo = FileInfo(
                language = SupportedLanguage.JAVA,
                physicalPath = "",
                content = ""
            )
            val cSharpFileInfo = FileInfo(
                language = SupportedLanguage.C_SHARP,
                physicalPath = "",
                content = ""
            )

            val typescriptFileInfo = FileInfo(
                language = SupportedLanguage.TYPESCRIPT,
                physicalPath = "",
                content = ""
            )
            val kotlinFileInfo = FileInfo(
                language = SupportedLanguage.KOTLIN,
                physicalPath = "",
                content = ""
            )
            return listOf(
                Arguments.of(javaFileInfo, JavaAnalyzer(javaFileInfo)),
                Arguments.of(cSharpFileInfo, CSharpAnalyzer(cSharpFileInfo)),
                Arguments.of(typescriptFileInfo, TypescriptAnalyzer(typescriptFileInfo)),
                Arguments.of(kotlinFileInfo, KotlinAnalyzer(kotlinFileInfo))
            )
        }
    }

    @ParameterizedTest
    @MethodSource("getFileInfoToAnalyzer")
    fun `returns the language of the analyzer`(
        fileInfo: FileInfo,
        expected: LanguageAnalyzer
    ) {
        // given & when
        val analyzer = LanguageAnalyzerFactory.createAnalyzer(fileInfo)

        // then
        assertThat(analyzer::class.java).isEqualTo(expected::class.java)
    }
}
