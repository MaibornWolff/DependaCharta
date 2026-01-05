package de.maibornwolff.dependacharta.pipeline.shared

enum class SupportedLanguage(
    val displayName: String,
    val suffixes: List<String>
) {
    PHP("PHP", listOf("php")),
    C_SHARP("C#", listOf("cs")),
    TYPESCRIPT("TypeScript", listOf("ts", "tsx")),
    JAVASCRIPT("JavaScript", listOf("js", "jsx")),
    JAVA("Java", listOf("java")),
    GO("Go", listOf("go")),
    PYTHON("Python", listOf("py")),
    CPP("C++", listOf("cpp", "c", "cc", "cxx", "h", "hpp", "hxx", "hh")),
    KOTLIN("Kotlin", listOf("kt", "kts")),
    VUE("Vue", listOf("vue")),
}

fun languagesByExtension(languages: List<SupportedLanguage>) =
    languages
        .flatMap { language ->
            language.suffixes.map { suffix -> suffix to language }
        }.toMap()

private const val NEL = "\u0085"

fun supportedLanguagesHelpText(): String {
    val languageLines = SupportedLanguage.entries.joinToString(NEL) { language ->
        val extensions = language.suffixes.joinToString(", ") { ".$it" }
        "  ${language.displayName} ($extensions)"
    }
    return "Supported Languages:$NEL$languageLines"
}
