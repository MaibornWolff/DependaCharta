package de.maibornwolff.dependacharta.pipeline.processing.dependencies

import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.processing.cycledetection.model.NodeInformation
import de.maibornwolff.dependacharta.pipeline.processing.dependencies.dictionaries.*
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage

class DependencyResolverService {
    companion object {
        fun resolveNodes(fileReports: Collection<FileReport>): Collection<Node> {
            val nodes = fileReports.flatMap { it.nodes }
            val dictionary = getDictionary(nodes)
            val knownNodePaths = getKnownNodePaths(nodes)
            return nodes.map { it.resolveTypes(dictionary, standardLibraryFor(it.language).get(), knownNodePaths) }
        }

        fun mapNodeInfo(node: Node) =
            NodeInformation(
                id = node.pathWithName.withDots(),
                dependencies = node.resolvedNodeDependencies.internalDependencies
                    .map { it.withDots() }
                    .toSet()
            )

        fun getDictionary(nodes: List<Node>) = nodes.map { it.pathWithName }.groupBy { it.parts.last() }

        fun getKnownNodePaths(nodes: List<Node>) = nodes.map { it.pathWithName.withDots() }.toSet()

        private fun standardLibraryFor(language: SupportedLanguage): StandardLibrary =
            when (language) {
                SupportedLanguage.JAVA -> JavaStandardLibrary()
                SupportedLanguage.C_SHARP -> CSharpStandardLibrary()
                SupportedLanguage.TYPESCRIPT -> EmptyStandardLibrary()
                SupportedLanguage.JAVASCRIPT -> EmptyStandardLibrary()
                SupportedLanguage.PHP -> EmptyStandardLibrary()
                SupportedLanguage.GO -> GoStandardLibrary()
                SupportedLanguage.PYTHON -> EmptyStandardLibrary()
                SupportedLanguage.CPP -> CppStandardLibrary()
                SupportedLanguage.KOTLIN -> KotlinStandardLibrary()
                SupportedLanguage.VUE -> EmptyStandardLibrary()
                SupportedLanguage.ABL -> EmptyStandardLibrary()
            }
    }
}
