package de.maibornwolff.dependacharta.pipeline

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import de.maibornwolff.dependacharta.pipeline.analysis.AnalysisPipeline
import de.maibornwolff.dependacharta.pipeline.processing.ProcessingPipeline
import de.maibornwolff.dependacharta.pipeline.shared.LogLevel
import de.maibornwolff.dependacharta.pipeline.shared.Logger
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.dependacharta.pipeline.shared.supportedLanguagesHelpText

fun main(args: Array<String>) {
    Cli().main(args)
}

class Cli : CliktCommand() {
    init {
        versionOption(
            version = VersionProvider().get().toString(),
            names = setOf("--version", "-v"),
            message = { "dependacharta Analysis $it" }
        )
    }

    override fun helpEpilog(context: com.github.ajalt.clikt.core.Context): String {
        return supportedLanguagesHelpText()
    }

    private val rootDirectory by option("-d", "--directory", help = "Root directory to analyze")
        .required()
    private val fileName by option("-f", "--filename", help = "Name of the generated cg.json file")
        .default("analysis")
    private val directoryName by option("-o", "--outputDirectory", help = "Relative path of output directory")
        .default("output")
    private val clean by option("-c", "--clean", help = "Clears the ongoing analysis and starts a new one")
        .flag(default = false)
    private val debug by option("-b", "--debug", help = "Keep the temp files")
        .flag(default = false)
    private val level by option(
        "-l",
        "--logLevel",
        help = "Define the log level for STDOUT. Possible values are debug, info, warn, error, fatal"
    ).default("info")
    private val omitGraphAnalysis by option(
        "-g",
        "--omitGraphAnalysis",
        help = "Don't traverse the graph to detect cycles etc."
    ).flag(default = false)
    private val maxFileSize by option(
        "-s",
        "--max-file-size",
        help = "Skip files larger than this (in KB). 0 = no limit"
    ).int().default(1024)
    private val fileTimeout by option(
        "-t",
        "--file-timeout",
        help = "Per-file analysis timeout in seconds. 0 = no timeout"
    ).int().default(60)
    private val excludeDir by option(
        "-x",
        "--exclude-dir",
        help = "Additional directory names to exclude (comma-separated), added to defaults"
    ).default("")
    private val excludeSuffix by option(
        "-X",
        "--exclude-suffix",
        help = "Additional file suffixes to exclude (comma-separated), added to defaults"
    ).default("")
    private val noDefaultExcludes by option(
        "--no-default-excludes",
        help = "Disable all default directory and suffix exclusions"
    ).flag(default = false)

    override fun run() {
        Logger.d("Current Directory: ${System.getProperty("user.dir")}")
        configureLogging()

        val excludedDirs = excludeDir.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val excludedSuffixes = excludeSuffix.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val fileReports = AnalysisPipeline.run(
            rootDirectory,
            clean,
            SupportedLanguage.entries.toList(),
            fileTimeoutSeconds = fileTimeout,
            maxFileSizeKB = maxFileSize,
            excludedDirs = excludedDirs,
            excludedSuffixes = excludedSuffixes,
            useDefaultExcludes = !noDefaultExcludes
        )
        ProcessingPipeline.run(
            fileName,
            directoryName,
            fileReports,
            omitGraphAnalysis
        )
        if (!debug) {
            AnalysisPipeline.cleanTempFiles()
        }
    }

    private fun configureLogging() {
        Logger.setLoggingDirectory(directoryName)

        when (level) {
            "debug" -> Logger.setLevel(LogLevel.DEBUG)
            "info" -> Logger.setLevel(LogLevel.INFO)
            "warn" -> Logger.setLevel(LogLevel.WARN)
            "error" -> Logger.setLevel(LogLevel.ERROR)
            "fatal" -> Logger.setLevel(LogLevel.FATAL)
            else -> Logger.setLevel(LogLevel.INFO)
        }
    }
}
