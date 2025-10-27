package de.maibornwolff.dependacharta.pipeline.analysis.analyzers

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.CppAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.CSharpAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.golang.GoAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.java.JavaAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.php.PhpAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.python.PythonAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.TypescriptAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage

class LanguageAnalyzerFactory {
    companion object {
        fun createAnalyzer(fileInfo: FileInfo): LanguageAnalyzer =
            when (fileInfo.language) {
                SupportedLanguage.JAVA -> JavaAnalyzer(fileInfo)
                SupportedLanguage.C_SHARP -> CSharpAnalyzer(fileInfo)
                SupportedLanguage.TYPESCRIPT -> TypescriptAnalyzer(fileInfo)
                SupportedLanguage.PHP -> PhpAnalyzer(fileInfo)
                SupportedLanguage.GO -> GoAnalyzer(fileInfo)
                SupportedLanguage.PYTHON -> PythonAnalyzer(fileInfo)
                SupportedLanguage.CPP -> CppAnalyzer(fileInfo)
            }
    }
}
