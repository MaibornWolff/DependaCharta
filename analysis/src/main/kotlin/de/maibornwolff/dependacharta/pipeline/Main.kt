package de.maibornwolff.dependacharta.pipeline

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.*
import de.maibornwolff.dependacharta.pipeline.analysis.AnalysisPipeline
import de.maibornwolff.dependacharta.pipeline.processing.ProcessingPipeline
import de.maibornwolff.dependacharta.pipeline.shared.LogLevel
import de.maibornwolff.dependacharta.pipeline.shared.Logger
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage

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

    override fun run() {
        Logger.d("Current Directory: ${System.getProperty("user.dir")}")
        configureLogging()

        val fileReports = AnalysisPipeline.run(
            rootDirectory,
            clean,
            listOf(
                SupportedLanguage.JAVA,
                SupportedLanguage.C_SHARP,
                SupportedLanguage.TYPESCRIPT,
                SupportedLanguage.JAVASCRIPT,
                SupportedLanguage.PHP,
                SupportedLanguage.GO,
                SupportedLanguage.PYTHON,
                SupportedLanguage.CPP,
                SupportedLanguage.KOTLIN,
                SupportedLanguage.VUE
            )
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
