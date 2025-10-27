package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model

sealed interface Import

data class RelativeImport(
    val relativePath: String
) : Import

data class DirectImport(
    val directPath: String
) : Import

fun String.toImport(): Import =
    if (startsWith("./") || startsWith("../")) {
        RelativeImport(this)
    } else {
        DirectImport(this)
    }
