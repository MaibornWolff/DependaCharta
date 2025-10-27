package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.getChildren
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency
import de.maibornwolff.codegraph.pipeline.analysis.model.Path
import de.maibornwolff.codegraph.pipeline.analysis.model.Type
import org.treesitter.TSNode

class UsingProcessor : CppNodeProcessor {
    override fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult {
        if (!nodeAsString(rootNode, context.source).contains("using")) {
            return ProcessorResult(emptyList(), context)
        }

        val isDerivedType = rootNode.getChild(2).type == "base_class_clause"
        if (isDerivedType) {
            return ProcessorResult(emptyList(), context)
        }

        val isNamespaceDirective = rootNode.getChild(1).grammarType == "namespace"
        if (isNamespaceDirective) {
            val namespaceDirective = nodeAsString(rootNode.getChild(2), context.source)
            return ProcessorResult(emptyList(), context.addAsDependency(namespaceDirective, isWildcard = true))
        }

        val isDeclarationForTypeOrMethod = rootNode.getChild(1).type == "qualified_identifier"
        if (isDeclarationForTypeOrMethod) {
            val typeWithNamespace = nodeAsString(rootNode.getChild(1), context.source)
            return ProcessorResult(emptyList(), context.addAsDependency(typeWithNamespace, isWildcard = false))
        }

        val isEnumDeclaration = rootNode.getChild(1).type == "enum"
        if (isEnumDeclaration) {
            val enumDeclaration = nodeAsString(rootNode.getChild(2), context.source)
            val hasNamespacePrefix = rootNode.getChildren().map { it.type }.contains("qualified_identifier") &&
                rootNode
                    .getChild(2)
                    .getChildren()
                    .map { it.type }
                    .contains("namespace_identifier")
            return if (hasNamespacePrefix) {
                ProcessorResult(emptyList(), context.addAsDependency(enumDeclaration, isWildcard = false))
            } else {
                ProcessorResult(emptyList(), context.addUsedTypes(setOf(Type.simple(enumDeclaration))))
            }
        }

        return ProcessorResult(emptyList(), context)
    }

    override fun appliesTo(node: TSNode) = node.type == "using_declaration"
}

private fun CppContext.addAsDependency(
    using: String,
    isWildcard: Boolean = false
) = this.addDependencies(listOf(Dependency(Path(using.split("::")), isWildcard = isWildcard)))
