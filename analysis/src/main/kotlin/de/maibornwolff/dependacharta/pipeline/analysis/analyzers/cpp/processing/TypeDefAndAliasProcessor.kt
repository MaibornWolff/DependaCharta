package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.CppQueryFactory
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.extractTypeWithFoundNamespacesAsDependencies
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.typeDeclarations
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import org.treesitter.TSNode

abstract class TypeDefAndAliasProcessor(
    val queryTemplate: String,
    val typeNodeIndex: Int
) : CppNodeProcessor {
    override fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult {
        val queryString = typeDeclarations().map { queryTemplate.replace("<TYPE>", it) }
        val combinedAliasAndTypeDefQuery = "[" + queryString.joinToString("\n") + "]"
        val query = CppQueryFactory.getQuery(combinedAliasAndTypeDefQuery)
        val nodes = mutableListOf<Node>()
        var enrichedContext = context.copy()
        for (match in rootNode.execute(query)) {
            val typeNode = match.captures[typeNodeIndex].node
            val (types, dependencies) = typeNode.extractTypeWithFoundNamespacesAsDependencies(context.source)
            val scopedContext = context.copy(usedTypes = types.toSet(), dependencies = dependencies)
            enrichedContext = enrichedContext
                .addUsedTypes(scopedContext.usedTypes)
                .addDependencies(scopedContext.getDependencies().toList())
        }
        return ProcessorResult(nodes, enrichedContext)
    }
}
