package de.maibornwolff.dependacharta.pipeline.analysis.model

import de.maibornwolff.dependacharta.pipeline.analysis.common.splitNameToParts
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import java.io.File

data class FileInfo(
    val language: SupportedLanguage,
    val physicalPath: String,
    val content: String,
    val analysisRoot: File? = null
) {
    fun physicalPathAsPath() = Path(splitNameToParts(physicalPath))
}
