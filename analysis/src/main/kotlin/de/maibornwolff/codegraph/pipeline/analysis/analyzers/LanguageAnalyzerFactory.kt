package de.maibornwolff.codegraph.pipeline.analysis.analyzers

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.CppAnalyzer
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.CSharpAnalyzer
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.golang.GoAnalyzer
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.java.JavaAnalyzer
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.php.PhpAnalyzer
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.python.PythonAnalyzer
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.TypescriptAnalyzer
import de.maibornwolff.codegraph.pipeline.analysis.model.FileInfo
import de.maibornwolff.codegraph.pipeline.shared.SupportedLanguage

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
