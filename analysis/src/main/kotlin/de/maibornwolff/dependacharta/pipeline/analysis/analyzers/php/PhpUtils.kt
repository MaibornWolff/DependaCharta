package de.maibornwolff.codegraph.pipeline.analysis.analyzers.php

fun String.trimFileEnding() =
    this.endsWith(".php").let {
        if (it) this.dropLast(4) else this
    }
