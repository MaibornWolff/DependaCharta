package de.maibornwolff.dependacharta.pipeline.analysis.model

import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage

fun Node.Companion.build(
    pathWithName: Path = Path(listOf("")),
    physicalPath: String = "",
    nodeType: NodeType = NodeType.CLASS,
    language: SupportedLanguage = SupportedLanguage.JAVA,
    dependencies: Set<Dependency> = setOf(),
    usedTypes: Set<Type> = setOf(),
    resolvedNodeDependencies: NodeDependencies = NodeDependencies(setOf(), setOf())
): Node =
    Node(
        pathWithName = pathWithName,
        physicalPath = physicalPath,
        nodeType = nodeType,
        language = language,
        dependencies = dependencies,
        usedTypes = usedTypes,
        resolvedNodeDependencies = resolvedNodeDependencies
    )