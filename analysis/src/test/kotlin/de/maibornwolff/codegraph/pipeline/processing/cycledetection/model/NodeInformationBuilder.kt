package de.maibornwolff.codegraph.pipeline.processing.cycledetection.model

fun NodeInformation.Companion.build(
    id: String = "",
    dependencies: Set<String> = setOf()
) = NodeInformation(id = id, dependencies = dependencies)
