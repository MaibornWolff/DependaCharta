package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import org.treesitter.TSNode
import org.treesitter.TreeSitterAbl

class AblDeclarationsQuery(
    val abl: TreeSitterAbl
) {
    fun execute(node: TSNode): List<TSNode> {
        return node
            .getNamedChildren()
            .filter { it.type == "class_definition" }
    }
}
