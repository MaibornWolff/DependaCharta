package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.extractTypeWithFoundNamespacesAsDependencies
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import de.maibornwolff.codegraph.pipeline.analysis.model.Type
import de.maibornwolff.codegraph.pipeline.analysis.model.TypeOfUsage
import org.treesitter.TSNode

class GenericTemplateProcessor : CppNodeProcessor {
    override fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult {
        var classContext = context.copy()
        val required = rootNode.getChild(2)
        var classDeclaration: TSNode?
        if (required.grammarType == "requires_clause") {
            // extract types to classContext
            var extracted = extractTypeConstraints(required.getChildByFieldName("constraint"), context.source)
            classContext = classContext.addUsedTypes(extracted)
            classDeclaration = rootNode.getChild(3)
        } else {
            classDeclaration = rootNode.getChild(2)
        }
        val templateResult = TypeDeclarationProcessor().process(classContext, classDeclaration)
        return ProcessorResult(templateResult.nodes, context)
    }

    private fun extractTypeConstraints(
        node: TSNode,
        source: String
    ): Set<Type> =
        if (node.type == "constraint_disjunction" || node.type == "constraint_conjunction") {
            extractTypeConstraints(node.getChildByFieldName("left"), source) +
                extractTypeConstraints(node.getChildByFieldName("right"), source)
        } else {
            node.extractTypeWithFoundNamespacesAsDependencies(source, TypeOfUsage.CONSTANT_ACCESS).component1().toSet()
        }

    override fun appliesTo(node: TSNode) = node.type == "template_declaration"
}
