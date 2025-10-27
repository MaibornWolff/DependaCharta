package de.maibornwolff.codegraph.pipeline.analysis.model

import de.maibornwolff.codegraph.pipeline.analysis.common.splitNameToParts
import de.maibornwolff.codegraph.pipeline.shared.SupportedLanguage

data class FileInfo(
    val language: SupportedLanguage,
    val physicalPath: String,
    val content: String
) {
    fun physicalPathAsPath() = Path(splitNameToParts(physicalPath))
}
