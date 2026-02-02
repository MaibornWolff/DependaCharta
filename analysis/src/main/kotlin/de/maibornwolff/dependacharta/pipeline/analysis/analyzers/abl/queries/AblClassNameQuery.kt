package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import org.treesitter.TSNode
import org.treesitter.TreeSitterAbl

class AblClassNameQuery(
    val abl: TreeSitterAbl
) {
    fun execute(
        classNode: TSNode,
        source: String
    ): Path? {
        // The first qualified_name child of class_definition is the class name
        val nameNode = classNode
            .getNamedChildren()
            .firstOrNull { it.type == "qualified_name" }
            ?: return null

        val qualifiedName = nodeAsString(nameNode, source)
        val parts = qualifiedName.split(".")
        return Path(parts)
    }
}
