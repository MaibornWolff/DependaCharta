package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.analysis.model.TypeOfUsage
import org.treesitter.TSNode

fun TSNode.extractTypeWithFoundNamespacesAsDependencies(
    nodeBody: String,
    typeOfUsage: TypeOfUsage = TypeOfUsage.USAGE
): Pair<List<Type>, Set<Dependency>> {
    if (this.isNull) {
        return listOf(Type.unparsable()) to emptySet()
    }
    var node = this
    if (node.type == "type_descriptor") {
        node = node.getChildByFieldName("type")
    }
    var namePrefix = Path(emptyList())
    while (node.type == "qualified_identifier") {
        val scope = node.getChildByFieldName("scope")
        if (!scope.isNull) {
            namePrefix = namePrefix.plus(nodeAsString(scope, nodeBody))
        }
        node = node.getChildByFieldName("name")
    }

    when (node.type) {
        "template_function" -> {
            val typeArguments = node.getNamedChild(1)
            val (genericTypes, dependenciesOfGenericTypes) = typeArguments
                .getNamedChildren()
                .map { it.extractTypeWithFoundNamespacesAsDependencies(nodeBody) }
                .unzip()
            val typeName = nodeAsString(node.getChildByFieldName("name"), nodeBody)
            val dependencies = dependenciesOfGenericTypes
                .flatten()
                .filter { it.path.parts.isNotEmpty() }
                .let {
                    if (namePrefix.parts.isEmpty()) {
                        it
                    } else {
                        it + Dependency(namePrefix, isWildcard = true)
                    }
                }.toSet()

            return listOf(Type.generic(typeName, genericTypes.flatten(), typeOfUsage)) to dependencies
        }

        "template_type" -> { // TODO: check  if this is needed inside of this function or can be declared where it is used.
            val arguments = extractTypeNameWithoutSizeSpecifier(node, nodeBody)
            val type = FunctionArgumentParser.intoTypes(arguments)
            val dependencies = FunctionArgumentParser.intoDependencies(arguments)
            return type to dependencies + if (namePrefix.parts.isEmpty()) {
                emptySet()
            } else {
                setOf(
                    Dependency(namePrefix, isWildcard = true)
                )
            }
        }
    }

    val typeName = extractTypeNameWithoutSizeSpecifier(node, nodeBody)
    return listOf(Type.simple(typeName, typeOfUsage)) to setOf(Dependency(namePrefix, isWildcard = true))
}

fun findIndexOfClosingDiamond(substring: String): Int {
    var openDiamonds = 0
    substring.forEach {
        when (it) {
            '<' -> openDiamonds++
            '>' -> {
                if (openDiamonds == 0) {
                    return substring.indexOf(it)
                } else {
                    openDiamonds--
                }
            }
        }
    }
    return substring.length
}

private fun extractTypeNameWithoutSizeSpecifier(
    node: TSNode,
    nodeBody: String
): String {
    if (node.type == "template_type") {
        return nodeAsString(node, nodeBody)
            .replace("unsigned", "")
            .replace("signed", "")
    }

    return findTypeNameNode(node, nodeBody)
        ?.let { nodeAsString(it, nodeBody) } ?: "int"
}

private fun findTypeNameNode(
    node: TSNode,
    nodeBody: String
): TSNode? {
    if (node.type != "sized_type_specifier") {
        return node
    }

    if (node.namedChildCount > 0) {
        return node.getNamedChild(0)
    }

    val sizeSpecifier = nodeAsString(node, nodeBody)
    return if (sizeSpecifier.lowercase() == "unsigned" || sizeSpecifier.lowercase() == "signed") {
        null
    } else {
        node
    }
}
