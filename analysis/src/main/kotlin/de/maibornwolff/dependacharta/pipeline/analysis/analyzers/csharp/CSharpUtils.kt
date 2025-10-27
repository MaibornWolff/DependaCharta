package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.csharp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.find
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.treesitter.TSNode

fun extractType(
    typeNode: TSNode,
    nodeBody: String
): Type {
    if (typeNode.isNull) {
        return Type.unparsable()
    }
    if (typeNode.type == "generic_name") {
        val typeIdentifier = typeNode.getNamedChild(0)
        val typeArguments = typeNode.getNamedChild(1)
        val genericTypes = typeArguments.getNamedChildren().map { extractType(it, nodeBody) }
        return Type.generic(nodeAsString(typeIdentifier, nodeBody), genericTypes)
    }
    if (typeNode.type == "qualified_name") {
        // Check if this qualified name ends with a generic_name
        val nameChild = typeNode.find("generic_name")
        if (nameChild != null) {
            // Extract the full qualified name up to the generic part
            // For qualified_name structure: qualifier.name, we need to get the qualifier part
            val qualifierNode = typeNode.getNamedChild(0) // first child is typically the qualifier
            val qualifierName = if (qualifierNode != null && qualifierNode.type != "generic_name") {
                nodeAsString(qualifierNode, nodeBody)
            } else {
                ""
            }

            // Extract the generic part
            val typeIdentifier = nameChild.getNamedChild(0)
            val typeArguments = nameChild.getNamedChild(1)
            val genericTypes = typeArguments.getNamedChildren().map { extractType(it, nodeBody) }

            val fullName = if (qualifierName.isNotEmpty()) {
                "$qualifierName.${nodeAsString(typeIdentifier, nodeBody)}"
            } else {
                nodeAsString(typeIdentifier, nodeBody)
            }

            return Type.generic(fullName, genericTypes)
        }
    }
    return Type.simple(nodeAsString(typeNode, nodeBody))
}
