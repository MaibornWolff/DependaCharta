package de.maibornwolff.codegraph.pipeline.analysis.analyzers.java

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.codegraph.pipeline.analysis.model.Type
import org.treesitter.TSNode

fun extractType(
    typeNode: TSNode,
    nodeBody: String
): Type {
    if (typeNode.isNull) return Type.unparsable()
    if (typeNode.type == "generic_type") {
        val typeIdentifier = typeNode.getNamedChild(0)
        val typeArguments = typeNode.getNamedChild(1)
        val genericTypes = typeArguments.getNamedChildren().map { extractType(it, nodeBody) }
        return Type.generic(nodeAsString(typeIdentifier, nodeBody), genericTypes)
    }
    return Type.simple(nodeAsString(typeNode, nodeBody))
}
