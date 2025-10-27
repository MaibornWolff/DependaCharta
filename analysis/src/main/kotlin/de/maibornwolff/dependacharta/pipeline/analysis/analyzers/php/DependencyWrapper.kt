package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.php

import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency

data class DependencyWrapper(
    val dependency: Dependency,
    val isConstant: Boolean = false
)
