package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.python

fun String.trimFileEnding() =
    this.endsWith(".py").let {
        if (it) this.dropLast(3) else this
    }
