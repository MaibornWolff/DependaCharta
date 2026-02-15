package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import org.treesitter.TSNode
import org.treesitter.TreeSitterAbl

class AblRunQuery(
    val abl: TreeSitterAbl
) {
    fun execute(
        rootNode: TSNode,
        source: String
    ): List<Dependency> {
        return rootNode
            .getNamedChildren()
            .filter { it.type == "run_statement" }
            .mapNotNull { runNode ->
                // Find the procedure_name child
                val procedureNode = runNode
                    .getNamedChildren()
                    .firstOrNull { it.type == "procedure_name" }
                    ?: return@mapNotNull null

                val procedurePath = nodeAsString(procedureNode, source)
                    .removeSuffix(".p")
                    .removeSuffix(".w")
                    .replace("/", ".")

                val parts = procedurePath.split(".")
                Dependency(path = Path(parts))
            }
    }
}
