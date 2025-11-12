package de.maibornwolff.dependacharta.pipeline.shared

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Path

class LoggerTest {
    @TempDir
    lateinit var tempDir: Path

    private val originalOut = System.out
    private val outputStream = ByteArrayOutputStream()

    @BeforeEach
    fun setup() {
        System.setOut(PrintStream(outputStream))

        Logger.setLevel(LogLevel.DEBUG)
        Logger.setLoggingDirectory(null)
    }

    @AfterEach
    fun cleanup() {
        System.setOut(originalOut)
        outputStream.reset()
    }

    @Test
    fun `should log all levels when DEBUG level is set`() {
        Logger.setLevel(LogLevel.DEBUG)

        Logger.d("Debug message")
        Logger.i("Info message")
        Logger.w("Warn message")
        Logger.e("Error message")
        Logger.f("Fatal message")

        val output = outputStream.toString()
        assertTrue(output.contains("[DEBUG] Debug message"))
        assertTrue(output.contains("[INFO] Info message"))
        assertTrue(output.contains("[WARN] Warn message"))
        assertTrue(output.contains("[ERROR] Error message"))
        assertTrue(output.contains("[FATAL] Fatal message"))
    }

    @Test
    fun `should filter messages based on log level`() {
        Logger.setLevel(LogLevel.WARN)

        Logger.d("Debug message")
        Logger.i("Info message")
        Logger.w("Warn message")
        Logger.e("Error message")

        val output = outputStream.toString()
        assertFalse(output.contains("[DEBUG]"))
        assertFalse(output.contains("[INFO]"))
        assertTrue(output.contains("[WARN] Warn message"))
        assertTrue(output.contains("[ERROR] Error message"))
    }

    @Test
    fun `should include color codes in console output`() {
        Logger.setLevel(LogLevel.DEBUG)

        Logger.d("Debug message")
        Logger.i("Info message")
        Logger.w("Warning message")
        Logger.e("Error message")

        val output = outputStream.toString()
        assertTrue(output.contains(BRIGHT_BLUE))
        assertTrue(output.contains(WHITE))
        assertTrue(output.contains(YELLOW))
        assertTrue(output.contains(RED))
        assertTrue(output.contains(RESET))
    }

    @Test
    fun `should write to file when logging directory is set`() {
        val logDir = tempDir.toFile()
        Logger.setLoggingDirectory(logDir.absolutePath)

        Logger.i("Test message")
        Logger.w("Warning message")

        val logFile = File(logDir, "analyzer.log")
        assertTrue(logFile.exists())

        val content = logFile.readText()
        assertTrue(content.contains("[INFO] Test message"))
        assertTrue(content.contains("[WARN] Warning message"))
        assertFalse(content.contains(WHITE))
        assertFalse(content.contains(RESET))
    }

    @Test
    fun `should create directory if it does not exist`() {
        val nonExistentDir = File(tempDir.toFile(), "new/nested/dir")
        assertFalse(nonExistentDir.exists())

        Logger.setLoggingDirectory(nonExistentDir.absolutePath)
        Logger.i("Test message")

        assertTrue(nonExistentDir.exists())
        val logFile = File(nonExistentDir, "analyzer.log")
        assertTrue(logFile.exists())
    }

    @Test
    fun `should delete existing log file when setting new directory`() {
        val logDir = tempDir.toFile()
        val logFile = File(logDir, "analyzer.log")

        logFile.writeText("Old content")
        assertTrue(logFile.exists())

        Logger.setLoggingDirectory(logDir.absolutePath)
        Logger.i("New message")

        val content = logFile.readText()
        assertFalse(content.contains("Old content"))
        assertTrue(content.contains("[INFO] New message"))
    }

    @Test
    fun `timed function should log start and end with duration`() {
        Logger.setLevel(LogLevel.INFO)

        val result = Logger.timed("Test operation") {
            Thread.sleep(10)
            "operation result"
        }

        assertEquals("operation result", result)

        System.out.flush()
        val output = outputStream.toString()
        // Debug: print what we actually got
        if (!output.contains("[⏳]")) {
            System.setOut(originalOut)
            println("DEBUG: Output does not contain expected emoji. Actual output:")
            println(output)
            println("DEBUG: Output bytes: ${output.toByteArray().joinToString(",")}")
            System.setOut(PrintStream(outputStream))
        }
        assertTrue(output.contains("'Test operation' started"))
        assertTrue(output.contains("'Test operation' took"))
    }

    @Test
    fun `timed function should return correct result even with exception`() {
        Logger.setLevel(LogLevel.INFO)

        assertThrows<RuntimeException> {
            Logger.timed("Failing operation") {
                throw RuntimeException("Test exception")
            }
        }

        System.out.flush()
        val output = outputStream.toString()
        assertTrue(output.contains("'Failing operation' started"))
    }

    @Test
    fun `log level enum comparison should work correctly`() {
        assertTrue(LogLevel.DEBUG.isIncludedIn(LogLevel.DEBUG))
        assertTrue(LogLevel.INFO.isIncludedIn(LogLevel.DEBUG))
        assertTrue(LogLevel.ERROR.isIncludedIn(LogLevel.WARN))

        assertFalse(LogLevel.DEBUG.isIncludedIn(LogLevel.INFO))
        assertFalse(LogLevel.WARN.isIncludedIn(LogLevel.ERROR))
    }

    @Test
    fun `should handle null logging directory gracefully`() {
        Logger.i("Test without file")

        val output = outputStream.toString()
        assertTrue(output.contains("[INFO] Test without file"))
    }
}