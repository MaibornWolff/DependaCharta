package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.dependacharta.pipeline.analysis.model.*
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage

fun typeDeclarations() =
    listOf(
        "primitive_type",
        "type_identifier",
        "template_type",
        "qualified_identifier",
    )

fun nodeType(type: String?): NodeType =
    when (type) {
        "struct_specifier" -> NodeType.VALUECLASS
        "union_specifier",
        "class_specifier" -> NodeType.CLASS
        "enum_specifier" -> NodeType.ENUM
        "type_definition",
        "alias_declaration" -> NodeType.UNKNOWN

        else -> NodeType.UNKNOWN
    }

fun createNode(
    name: String,
    nodeType: NodeType,
    usedTypes: Set<Type>,
    dependencies: Set<Dependency>,
    namespace: Path,
    physicalPath: String
): Node {
    val alias = Path.fromPhysicalPath(physicalPath)
    return Node(
        pathWithName = (namespace + listOf(name)).withAlias(alias),
        physicalPath = physicalPath,
        language = SupportedLanguage.CPP,
        nodeType = nodeType,
        dependencies = dependencies + listOf(Dependency(namespace, isWildcard = true)),
        usedTypes = usedTypes
    )
}

fun createNode(
    context: CppContext,
    name: String,
    nodeType: NodeType
): Node =
    createNode(
        name,
        nodeType,
        context.usedTypes,
        context.getDependencies(),
        context.nameSpace,
        context.fileInfo.physicalPath
    )

internal fun String.transformFileEnding() = this.replace(".", "_")
