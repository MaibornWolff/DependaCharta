package de.maibornwolff.codegraph.pipeline.shared

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ConditionalProgressBarTest {
    private val originalOut = System.out
    private val outputStream = ByteArrayOutputStream()

    @BeforeEach
    fun setup() {
        System.setOut(PrintStream(outputStream))
        Logger.setLevel(LogLevel.DEBUG)
    }

    @AfterEach
    fun cleanup() {
        System.setOut(originalOut)
        outputStream.reset()
    }

    @Test
    fun `should show progress bar when condition is true`() {
        val progressBar = ConditionalProgressBar("Test Task", 100) { true }
        var stepCallCount = 0

        val result = progressBar.use { step ->
            repeat(5) {
                step()
                stepCallCount++
            }
            "task completed"
        }

        assertEquals("task completed", result)
        assertEquals(5, stepCallCount)

        val output = outputStream.toString()
        assertTrue(output.contains("[WARN] 'Test Task' consists of 100 steps. This might take a while."))
    }

    @Test
    fun `should not show progress bar when condition is false`() {
        val progressBar = ConditionalProgressBar("Test Task", 100) { false }
        var stepCallCount = 0

        val result = progressBar.use { step ->
            step()
            step()
            stepCallCount += 2
            "task completed without progress"
        }

        assertEquals("task completed without progress", result)
        assertEquals(2, stepCallCount)

        val output = outputStream.toString()
        assertFalse(output.contains("This might take a while"))
    }

    @Test
    fun `should return correct result from block`() {
        val progressBar = ConditionalProgressBar("Math Task", 10) { true }

        val result = progressBar.use { step ->
            val sum = (1..5).sum()
            step()
            sum * 2
        }

        assertEquals(30, result) // (1+2+3+4+5) * 2 = 30
    }

    @Test
    fun `should handle exceptions from block`() {
        val progressBar = ConditionalProgressBar("Failing Task", 5) { true }

        assertThrows<RuntimeException> {
            progressBar.use { step ->
                step()
                throw RuntimeException("Test exception")
            }
        }

        val output = outputStream.toString()
        assertTrue(output.contains("[WARN] 'Failing Task' consists of 5 steps"))
    }

    @Test
    fun `should work with dynamic condition`() {
        var shouldShow = false
        val progressBar = ConditionalProgressBar("Dynamic Task", 3) { shouldShow }

        var result1 = progressBar.use { step ->
            step()
            "first run"
        }

        assertEquals("first run", result1)

        shouldShow = true

        var result2 = progressBar.use { step ->
            step()
            "second run"
        }

        assertEquals("second run", result2)

        val output = outputStream.toString()
        assertTrue(output.contains("[WARN] 'Dynamic Task' consists of 3 steps"))
    }

    @Test
    fun `should handle zero steps`() {
        val progressBar = ConditionalProgressBar("Empty Task", 0) { true }

        val result = progressBar.use { step ->
            "no steps needed"
        }

        assertEquals("no steps needed", result)

        val output = outputStream.toString()
        assertTrue(output.contains("[WARN] 'Empty Task' consists of 0 steps"))
    }

    @Test
    fun `should handle large number of steps`() {
        val progressBar = ConditionalProgressBar("Big Task", 1000000) { true }

        val result = progressBar.use { step ->
            repeat(3) { step() }
            "big task done"
        }

        assertEquals("big task done", result)

        val output = outputStream.toString()
        assertTrue(output.contains("[WARN] 'Big Task' consists of 1000000 steps"))
    }

    @Test
    fun `should work with empty step function when condition is false`() {
        val progressBar = ConditionalProgressBar("No Progress Task", 10) { false }
        var blockExecuted = false

        val result = progressBar.use { step ->
            blockExecuted = true
            step()
            step()
            "executed"
        }

        assertTrue(blockExecuted)
        assertEquals("executed", result)

        val output = outputStream.toString()
        assertFalse(output.contains("This might take a while"))
    }

    @Test
    fun `should preserve name and steps in warning message`() {
        val taskName = "Complex Analysis Task"
        val stepCount = 42
        val progressBar = ConditionalProgressBar(taskName, stepCount) { true }

        progressBar.use { step ->
            step()
            "done"
        }

        val output = outputStream.toString()
        assertTrue(output.contains("'$taskName' consists of $stepCount steps"))
    }

    @Test
    fun `condition should be evaluated each time use is called`() {
        var conditionCallCount = 0
        val progressBar = ConditionalProgressBar("Condition Test", 5) {
            conditionCallCount++
            conditionCallCount <= 2
        }

        progressBar.use { step -> "first" }
        assertEquals(1, conditionCallCount)

        progressBar.use { step -> "second" }
        assertEquals(2, conditionCallCount)

        progressBar.use { step -> "third" }
        assertEquals(3, conditionCallCount)

        val output = outputStream.toString()
        val warnCount = output.split("This might take a while").size - 1
        assertEquals(2, warnCount)
    }
}