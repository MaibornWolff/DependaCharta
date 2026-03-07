package de.maibornwolff.dependacharta.pipeline.analysis.analyzers

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.AblAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.CppAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.CSharpAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.golang.GoAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.java.JavaAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.JavascriptAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin.KotlinAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.php.PhpAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.python.PythonAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.TypescriptAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.vue.VueAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage

class LanguageAnalyzerFactory {
    companion object {
        fun createAnalyzer(fileInfo: FileInfo): LanguageAnalyzer =
            when (fileInfo.language) {
                SupportedLanguage.JAVA -> JavaAnalyzer(fileInfo)
                SupportedLanguage.C_SHARP -> CSharpAnalyzer(fileInfo)
                SupportedLanguage.TYPESCRIPT -> TypescriptAnalyzer(fileInfo)
                SupportedLanguage.JAVASCRIPT -> JavascriptAnalyzer(fileInfo)
                SupportedLanguage.PHP -> PhpAnalyzer(fileInfo)
                SupportedLanguage.GO -> GoAnalyzer(fileInfo)
                SupportedLanguage.PYTHON -> PythonAnalyzer(fileInfo)
                SupportedLanguage.CPP -> CppAnalyzer(fileInfo)
                SupportedLanguage.KOTLIN -> KotlinAnalyzer(fileInfo)
                SupportedLanguage.VUE -> VueAnalyzer(fileInfo)
                SupportedLanguage.ABL -> AblAnalyzer(fileInfo)
            }
    }
}
