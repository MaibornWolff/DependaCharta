package de.maibornwolff.dependacharta.pipeline.processing.model

fun ProjectNodeDto.Companion.build(
    leafId: String? = null,
    name: String = "",
    children: Set<ProjectNodeDto> = emptySet(),
    level: Int = 0,
    containedLeaves: Set<String> = emptySet(),
    containedInternalDependencies: Map<String, EdgeInfoDto> = emptyMap()
) = ProjectNodeDto(
    leafId = leafId,
    name = name,
    children = children,
    level = level,
    containedLeaves = containedLeaves,
    containedInternalDependencies = containedInternalDependencies
)