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
            return nodes.map { it.resolveTypes(dictionary, dictionary(it.language).get(), knownNodePaths) }
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

        private fun dictionary(language: SupportedLanguage): LanguageDictionary =
            when (language) {
                SupportedLanguage.JAVA -> JavaDictionary()
                SupportedLanguage.C_SHARP -> CSharpDictionary()
                SupportedLanguage.TYPESCRIPT -> TypescriptDictionary()
                SupportedLanguage.PHP -> PhpDictionary()
                SupportedLanguage.GO -> GoDictionary()
                SupportedLanguage.PYTHON -> PythonDictionary()
                SupportedLanguage.CPP -> CppDictionary()
            }
    }
}
