package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.CppQueryFactory
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.extractTypeWithFoundNamespacesAsDependencies
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.analysis.model.TypeOfUsage
import org.treesitter.TSNode

class InheritanceProcessor : CppNodeProcessor {
    val inheritanceQueryString = """
[
(class_specifier
  (base_class_clause)  @decl.superclass
)
(struct_specifier
  (base_class_clause)  @decl.superclass
)
]
"""

    override fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult {
        val inheritanceQuery = CppQueryFactory.getQuery(inheritanceQueryString)
        val (superclasses, dependencies) = rootNode
            .execute(inheritanceQuery)
            .flatMap { extractSuperClassType(it.captures[0].node, context.source) }
            .unzip()
        var enrichedContext = context.copy()
        enrichedContext = enrichedContext.addUsedTypes(superclasses.flatten().toSet())
        enrichedContext = enrichedContext.addDependencies(dependencies.flatten().toSet())

        return ProcessorResult(emptyList(), enrichedContext)
    }

    private fun extractSuperClassType(
        typeNode: TSNode,
        source: String
    ): List<Pair<List<Type>, Set<Dependency>>> {
        val result = mutableListOf<Pair<List<Type>, Set<Dependency>>>()
        for (i in 0 until typeNode.childCount) {
            val node = typeNode.getChild(i)
            if (node.type == "access_specifier" || !node.isNamed) continue
            result.add(node.extractTypeWithFoundNamespacesAsDependencies(source, TypeOfUsage.INHERITANCE))
        }
        return result
    }

    override fun appliesTo(node: TSNode) = node.type == "class_specifier" || node.type == "struct_specifier"
}
