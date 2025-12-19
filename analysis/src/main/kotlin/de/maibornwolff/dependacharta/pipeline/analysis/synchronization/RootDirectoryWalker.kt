package de.maibornwolff.dependacharta.pipeline.analysis.synchronization

import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.dependacharta.pipeline.shared.languagesByExtension
import java.io.File

class RootDirectoryWalker(
    public val rootDirectory: File,
    private val languages: List<SupportedLanguage>
) {
    fun walk(): Sequence<String> {
        val languagesByExtension = languagesByExtension(languages)
        val ftw = rootDirectory.walk().filter { shouldAnalyze(it, languagesByExtension) }
        return ftw.map { it.path }
    }

    fun getFileInfo(path: String): FileInfo {
        val sourceCodeFile = File(path)
        val relativePath = sourceCodeFile.toRelativeString(rootDirectory)
        // If relative path is empty (single file analysis), use the file name
        val effectivePath = if (relativePath.isEmpty()) sourceCodeFile.name else relativePath
        return FileInfo(
            language = determineLanguage(path),
            physicalPath = effectivePath,
            content = sourceCodeFile
                .readText(Charsets.UTF_8)
                // delete UTF-8 BOM character
                .removePrefix("\uFEFF"),
            analysisRoot = rootDirectory
        )
    }

    private fun shouldAnalyze(
        file: File,
        languagesByExtension: Map<String, SupportedLanguage>
    ): Boolean {
        if (!file.isFile) return false
        val extension = file.extension
        if (!languagesByExtension.containsKey(extension)) return false
        val relativePath = file.toRelativeString(rootDirectory).replace("\\", "/")
        return isNotIgnored(relativePath)
    }

    private fun isNotIgnored(path: String): Boolean {
        val pathParts = path.split("/")
        val isInIgnoredDirectory = ignoredDirectories().any { pathParts.contains(it) }
        val isIgnoredFileEnding = ignoredFileEndings().any { path.endsWith(it) }
        return !isInIgnoredDirectory && !isIgnoredFileEnding
    }

    private fun determineLanguage(absolutePath: String): SupportedLanguage {
        val suffix = absolutePath.split(".").last()
        return SupportedLanguage.entries.find { it.suffixes.contains(suffix) }
            ?: throw IllegalArgumentException("Language for file $absolutePath could not be determined")
    }
}
