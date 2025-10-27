package de.maibornwolff.codegraph.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VersionProviderTest {
    @Test
    fun `should return version from version file`() {
        // given
        val versionProvider = VersionProvider()

        // when
        val version = versionProvider.get()

        // then
        assertEquals("0.0.0-test", version.version)
    }
}
