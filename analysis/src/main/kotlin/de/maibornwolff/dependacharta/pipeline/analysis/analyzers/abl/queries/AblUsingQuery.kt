package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import org.treesitter.TSNode
import org.treesitter.TreeSitterAbl

class AblUsingQuery(
    val abl: TreeSitterAbl
) {
    fun execute(
        rootNode: TSNode,
        source: String
    ): List<Dependency> {
        return rootNode
            .getNamedChildren()
            .filter { it.type == "using_statement" }
            .mapNotNull { usingNode ->
                val usingText = nodeAsString(usingNode, source)
                    .removePrefix("USING")
                    .removeSuffix(".")
                    .trim()

                if (usingText.endsWith("*")) {
                    val pathWithoutWildcard = usingText.dropLast(2).trim()
                    val parts = pathWithoutWildcard.split(".")
                    Dependency(path = Path(parts), isWildcard = true)
                } else {
                    val parts = usingText.split(".")
                    Dependency(path = Path(parts))
                }
            }
    }
}
