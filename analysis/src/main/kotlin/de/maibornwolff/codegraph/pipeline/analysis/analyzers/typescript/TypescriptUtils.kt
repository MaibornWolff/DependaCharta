package de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript

fun String.trimFileEnding() =
    this.endsWith(".ts").let {
        if (it) this.dropLast(3) else this
    }
