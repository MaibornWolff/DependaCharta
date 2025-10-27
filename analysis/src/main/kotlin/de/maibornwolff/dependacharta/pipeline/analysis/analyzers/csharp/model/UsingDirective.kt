package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp.model

data class UsingDirective(
    val path: List<String>,
    val alias: String?
)
