package de.maibornwolff.dependacharta.pipeline.analysis.model

import de.maibornwolff.dependacharta.pipeline.analysis.common.splitNameToParts
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage

data class FileInfo(
    val language: SupportedLanguage,
    val physicalPath: String,
    val content: String
) {
    fun physicalPathAsPath() = Path(splitNameToParts(physicalPath))
}
