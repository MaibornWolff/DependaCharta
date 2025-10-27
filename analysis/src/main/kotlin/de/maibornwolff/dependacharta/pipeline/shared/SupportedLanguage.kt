package de.maibornwolff.dependacharta.pipeline.shared

enum class SupportedLanguage(
    val suffixes: List<String>
) {
    PHP(listOf("php")),
    C_SHARP(listOf("cs")),
    TYPESCRIPT(listOf("ts")),
    JAVA(listOf("java")),
    GO(listOf("go")),
    PYTHON(listOf("py")),
    CPP(listOf("cpp", "c", "cc", "cxx", "h", "hpp", "hxx", "hh")),
}

fun languagesByExtension(languages: List<SupportedLanguage>) =
    languages
        .flatMap { language ->
            language.suffixes.map { suffix -> suffix to language }
        }.toMap()
