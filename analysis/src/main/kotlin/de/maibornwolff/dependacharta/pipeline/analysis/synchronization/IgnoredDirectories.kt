package de.maibornwolff.dependacharta.pipeline.analysis.synchronization

fun ignoredDirectories() = listOf("node_modules", "dist", "build", "test", "tests", "__tests__")

fun ignoredFileEndings() = listOf(".spec.ts", "_test.go", "Test.java", "Test.kt")