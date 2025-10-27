package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.CppQueryFactory
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import org.treesitter.TSNode

class NamespaceProcessor : CppNodeProcessor {
    val namespaceDeclarationQueryString =
"""
(namespace_definition 
  name : (namespace_identifier) @name
  body : (declaration_list) @body
  )
  (namespace_definition 
  name : (nested_namespace_specifier) @name
  body : (declaration_list) @body
  )
 (namespace_definition 
  body : (declaration_list) @body
  )
"""

    val namespaceDeclaration = "namespace_definition"

    override fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult {
        val namespaceDeclarationQuery = CppQueryFactory.getQuery(namespaceDeclarationQueryString)
        val namespaceMatch = rootNode.execute(namespaceDeclarationQuery).first()
        val potentialNamespaceNode = namespaceMatch.captures[0].node
        val bodyNode: TSNode?
        val scopedContext: CppContext

        if (potentialNamespaceNode.type == "declaration_list") {
            bodyNode = potentialNamespaceNode
            scopedContext = context.copy()
        } else {
            val nameOfNamespace = nodeAsString(namespaceMatch.captures[0].node, context.source).split("::")
            scopedContext = context.addNamespace(nameOfNamespace)
            bodyNode = namespaceMatch.captures[1].node
        }

        val namespaceProcessorResult = BodyProcessor().process(scopedContext, bodyNode)
        // TODO: Why is contex returned and not scopedContext bzw. the context of namespaceProcessorResult?
        return ProcessorResult(namespaceProcessorResult.nodes, context)
    }

    override fun appliesTo(node: TSNode) = node.type == namespaceDeclaration
}
