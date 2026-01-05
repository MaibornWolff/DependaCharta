package de.maibornwolff.dependacharta.pipeline.shared

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SupportedLanguageTest {
    @Test
    fun `displayName returns human-readable name for C_SHARP`() {
        // Given
        val language = SupportedLanguage.C_SHARP

        // When
        val displayName = language.displayName

        // Then
        assertThat(displayName).isEqualTo("C#")
    }

    @Test
    fun `displayName returns human-readable name for CPP`() {
        // Given
        val language = SupportedLanguage.CPP

        // When
        val displayName = language.displayName

        // Then
        assertThat(displayName).isEqualTo("C++")
    }

    @Test
    fun `displayName returns human-readable name for TYPESCRIPT`() {
        // Given
        val language = SupportedLanguage.TYPESCRIPT

        // When
        val displayName = language.displayName

        // Then
        assertThat(displayName).isEqualTo("TypeScript")
    }

    @Test
    fun `all languages have a displayName`() {
        // Given & When & Then
        SupportedLanguage.entries.forEach { language ->
            assertThat(language.displayName).isNotBlank()
        }
    }

    @Test
    fun `supportedLanguagesHelpText contains all languages with extensions`() {
        // Given & When
        val helpText = supportedLanguagesHelpText()

        // Then
        assertThat(helpText).contains("Java")
        assertThat(helpText).contains(".java")
        assertThat(helpText).contains("TypeScript")
        assertThat(helpText).contains(".ts")
        assertThat(helpText).contains(".tsx")
        assertThat(helpText).contains("C#")
        assertThat(helpText).contains(".cs")
    }

    @Test
    fun `supportedLanguagesHelpText has header`() {
        // Given & When
        val helpText = supportedLanguagesHelpText()

        // Then
        assertThat(helpText).startsWith("Supported Languages:")
    }
}
