package de.maibornwolff.codegraph.pipeline.analysis.analyzers.php

import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency

data class DependencyWrapper(
    val dependency: Dependency,
    val isConstant: Boolean = false
)
