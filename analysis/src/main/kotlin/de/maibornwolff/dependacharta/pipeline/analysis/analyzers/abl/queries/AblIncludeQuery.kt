package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import org.treesitter.TSNode
import org.treesitter.TreeSitterAbl

class AblIncludeQuery(
    val abl: TreeSitterAbl
) {
    fun execute(
        rootNode: TSNode,
        source: String
    ): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()

        rootNode.getNamedChildren().forEach { child ->
            when (child.type) {
                "include_extra" -> {
                    // Simple include: {include/path.i}
                    val includeText = nodeAsString(child, source)
                    extractIncludePath(includeText)?.let { dependencies.add(it) }
                }
                "include" -> {
                    // Include with arguments: {path.i &arg=value}
                    val fileRef = child
                        .getNamedChildren()
                        .firstOrNull { it.type == "include_file_reference" }
                    val fileName = fileRef
                        ?.getNamedChildren()
                        ?.firstOrNull { it.type == "file_name" }

                    if (fileName != null) {
                        val path = nodeAsString(fileName, source)
                        convertPathToDependency(path)?.let { dependencies.add(it) }
                    }
                }
            }
        }

        return dependencies
    }

    private fun extractIncludePath(includeText: String): Dependency? {
        // Remove braces and trailing newlines: {include/path.i}\n -> include/path.i
        val path = includeText
            .trim()
            .removePrefix("{")
            .removeSuffix("}")
            .trim()

        return convertPathToDependency(path)
    }

    private fun convertPathToDependency(path: String): Dependency? {
        if (path.isBlank()) return null

        // Convert path: include/error-handling.i -> include.error-handling
        val normalizedPath = path
            .removeSuffix(".i")
            .replace("/", ".")

        val parts = normalizedPath.split(".")
        return Dependency(path = Path(parts))
    }
}
