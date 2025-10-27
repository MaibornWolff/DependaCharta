package de.maibornwolff.dependacharta.pipeline.processing.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectReportDto(
    val projectTreeRoots: Set<ProjectNodeDto>,
    val leaves: Map<String, LeafInformationDto>
)

@Serializable
data class ProjectNodeDto(
    val leafId: String?,
    val name: String,
    val children: Set<ProjectNodeDto>,
    val level: Int,
    val containedLeaves: Set<String>,
    val containedInternalDependencies: Map<String, EdgeInfoDto>,
)

@Serializable
data class LeafInformationDto(
    val id: String,
    val name: String,
    val physicalPath: String,
    val nodeType: String,
    val language: String,
    val dependencies: Map<String, EdgeInfoDto>,
)

@Serializable
data class EdgeInfoDto(
    val isCyclic: Boolean,
    val weight: Int,
    val type: String
)
