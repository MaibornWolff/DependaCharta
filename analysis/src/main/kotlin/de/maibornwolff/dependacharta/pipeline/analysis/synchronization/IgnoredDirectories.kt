package de.maibornwolff.dependacharta.pipeline.analysis.synchronization

fun ignoredDirectories() = listOf("node_modules", "dist", "build")

fun ignoredFileEndings() = listOf(".spec.ts", "_test.go")