package de.maibornwolff.dependacharta.pipeline.processing.dependencies

import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.processing.cycledetection.model.NodeInformation
import de.maibornwolff.dependacharta.pipeline.processing.dependencies.dictionaries.*
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage

class DependencyResolverService {
    companion object {
        fun resolveNodes(fileReports: Collection<FileReport>): Collection<Node> {
            val nodes = applyRustReexportAliases(fileReports.flatMap { it.nodes })
            val dictionary = getDictionary(nodes)
            val knownNodePaths = getKnownNodePaths(nodes)
            return nodes.map { it.resolveTypes(dictionary, standardLibraryFor(it.language).get(), knownNodePaths) }
        }

        /**
         * Folds Rust `pub use` re-export forwarding nodes into aliases on their real definition node,
         * then drops the forwarding nodes. A consumer importing the flattened `crate::Type` path then
         * resolves straight to `crate::module::Type` via the alias — instead of through a same-named
         * forwarding node, which would otherwise capture the definition's own self-references and
         * fabricate `crate::Type ↔ crate::module::Type` cycles.
         */
        private fun applyRustReexportAliases(nodes: List<Node>): List<Node> {
            val (reexports, rest) = nodes.partition {
                it.language == SupportedLanguage.RUST && it.nodeType == NodeType.REEXPORT
            }
            if (reexports.isEmpty()) return nodes
            val byPath = rest.associateBy { it.pathWithName.withDots() }
            reexports.forEach { reexport ->
                val target = reexport.dependencies.firstOrNull { !it.isWildcard }?.path ?: return@forEach
                byPath[target.withDots()]?.pathWithName?.withAlias(reexport.pathWithName)
            }
            return rest
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
                SupportedLanguage.DELPHI -> EmptyStandardLibrary()
                SupportedLanguage.RUST -> EmptyStandardLibrary()
            }
    }
}
