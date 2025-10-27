package de.maibornwolff.dependacharta.pipeline.shared

import java.io.File
import kotlin.time.measureTimedValue

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL
}

const val RESET = "\u001B[0m"
const val RED = "\u001B[31m"
const val YELLOW = "\u001B[33m"
const val WHITE = "\u001B[37m"
const val BRIGHT_BLUE = "\u001B[94m"

fun LogLevel.isIncludedIn(level: LogLevel): Boolean = this >= level

class Logger {
    companion object {
        private var loggingFilename: String? = null
        private var logLevel: LogLevel = LogLevel.INFO

        @Synchronized
        fun setLoggingDirectory(loggingDirectory: String?) {
            if (loggingDirectory == null) {
                loggingFilename = null
                return
            }

            loggingFilename = "$loggingDirectory/analyzer.log"
            File(loggingFilename!!).delete()

            if (!File(loggingDirectory).exists()) {
                File(loggingDirectory).mkdirs()
            }
        }

        @Synchronized
        fun setLevel(level: LogLevel) {
            this.logLevel = level
        }

        fun d(message: String) {
            log(message, LogLevel.DEBUG)
        }

        fun i(message: String) {
            log(message, LogLevel.INFO)
        }

        fun w(message: String) {
            log(message, LogLevel.WARN)
        }

        fun e(message: String) {
            log(message, LogLevel.ERROR)
        }

        fun f(message: String) {
            log(message, LogLevel.FATAL)
        }

        fun <T> timed(
            description: String,
            block: () -> T
        ): T {
            i("[⏳] '$description' started")
            val timedValue = measureTimedValue(block)
            i("[⌛] '$description' took ${timedValue.duration}")
            return timedValue.value
        }

        private fun log(
            message: String,
            level: LogLevel
        ) {
            if (level.isIncludedIn(logLevel)) {
                println(buildVisualLogline(message, level))
            }
            appendToFile(buildFileLogline(message, level))
        }

        private fun appendToFile(line: String) {
            if (loggingFilename != null) {
                File(loggingFilename!!).appendText(line + '\n')
            }
        }

        private fun buildVisualLogline(
            message: String,
            level: LogLevel
        ): String = "${chooseColor(level)} ${buildLevelBlock(level)} $message$RESET"

        private fun buildFileLogline(
            message: String,
            level: LogLevel
        ): String = "${buildLevelBlock(level)} $message"

        private fun chooseColor(level: LogLevel): String =
            when (level) {
                LogLevel.DEBUG -> BRIGHT_BLUE
                LogLevel.INFO -> WHITE
                LogLevel.WARN -> YELLOW
                LogLevel.ERROR -> RED
                LogLevel.FATAL -> RED
            }

        private fun buildLevelBlock(level: LogLevel): String =
            when (level) {
                LogLevel.DEBUG -> "[DEBUG]"
                LogLevel.INFO -> "[INFO]"
                LogLevel.WARN -> "[WARN]"
                LogLevel.ERROR -> "[ERROR]"
                LogLevel.FATAL -> "[FATAL]"
            }
    }
}