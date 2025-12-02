package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.treesitter.TSNode

fun extractType(
    typeNode: TSNode,
    nodeBody: String
): Type {
    if (typeNode.isNull) return Type.unparsable()

    return when (typeNode.type) {
        "user_type" -> extractUserType(typeNode, nodeBody)
        "nullable_type" -> extractType(typeNode.getNamedChild(0), nodeBody)
        else -> Type.simple(nodeAsString(typeNode, nodeBody))
    }
}

private fun extractUserType(
    typeNode: TSNode,
    nodeBody: String
): Type {
    val typeIdentifier = typeNode.getNamedChild(0)
    val typeArguments = typeNode.getNamedChild(1)

    if (typeNode.namedChildCount <= 1 || typeArguments.type != "type_arguments") {
        return Type.simple(nodeAsString(typeIdentifier, nodeBody))
    }

    val genericTypes = typeArguments
        .getNamedChildren()
        .filter { it.type == "type_projection" }
        .mapNotNull { it.getNamedChildren().firstOrNull() }
        .map { extractType(it, nodeBody) }

    return Type.generic(nodeAsString(typeIdentifier, nodeBody), genericTypes)
}
