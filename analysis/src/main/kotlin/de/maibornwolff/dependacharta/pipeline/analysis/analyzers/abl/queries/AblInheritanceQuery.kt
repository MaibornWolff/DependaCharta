package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.treesitter.TSNode
import org.treesitter.TreeSitterAbl

class AblInheritanceQuery(
    val abl: TreeSitterAbl
) {
    fun execute(
        classNode: TSNode,
        source: String
    ): Set<Type> {
        val children = classNode.getNamedChildren()

        // Find the index of the first qualified_name (the class name)
        val classNameIndex = children.indexOfFirst { it.type == "qualified_name" }
        if (classNameIndex == -1) return emptySet()

        // Collect all qualified_name and identifier nodes AFTER the class name
        // These represent INHERITS and IMPLEMENTS types
        return children
            .drop(classNameIndex + 1)
            .filter { it.type == "qualified_name" || it.type == "identifier" }
            .map { node ->
                val typeName = nodeAsString(node, source)
                val simpleName = typeName.substringAfterLast(".")
                Type.simple(simpleName)
            }.toSet()
    }
}
