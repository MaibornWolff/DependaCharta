package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.CppQueryFactory
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.extractTypeWithFoundNamespacesAsDependencies
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.typeDeclarations
import de.maibornwolff.dependacharta.pipeline.analysis.model.TypeOfUsage
import org.treesitter.TSNode

class VariableDeclarationProcessor : CppNodeProcessor {
    companion object {
        val usedTypeOccurencyQueryString: String = createUsedTypeOccurenceQuery()

        private fun createUsedTypeOccurenceQuery(): String {
            val templateFunctionCall = """(template_function 
        arguments: (template_argument_list (type_descriptor) @type)
        )"""
            // TODO: Check if tentative mechanism is still needed for this. needs to be reintroduced if so.
            val constructorCall =
                "[(call_expression function: (qualified_identifier) @type) (argument_list (call_expression function: (identifier)@potential_constructor))]"
            val staticFunctionCall =
                "(call_expression function: (qualified_identifier scope: (namespace_identifier)@type))"
            val typeCast = "(cast_expression type: (type_descriptor) @type)"
            val throwCalls = listOf("primitive_type", "identifier", "qualified_identifier", "template_function")
                .map { type -> "(throw_statement (call_expression function: ($type) @type))" }
            val declarationTypes = listOf("field_declaration", "parameter_declaration", "declaration")
            val matchAllDeclarationsQueries = declarationTypes.flatMap { decl ->
                typeDeclarations().map { type -> "($decl type:($type) @type)" }
            } + throwCalls + listOf(
                constructorCall +
                    staticFunctionCall +
                    templateFunctionCall +
                    typeCast
            )
            return matchAllDeclarationsQueries
                .joinToString("\n")
        }
    }

    override fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult {
        try {
            val usedTypesQuery = CppQueryFactory.getQuery(usedTypeOccurencyQueryString)
            var enrichedContext = context.copy()
            val result = rootNode.execute(usedTypesQuery)
            val (usedTypes, namespacesOfTypes) = result
                .map {
                    it.captures[0].node.extractTypeWithFoundNamespacesAsDependencies(
                        context.source,
                        determineUsage(it.captures[0].node)
                    )
                }.unzip()

            enrichedContext = enrichedContext.addDependencies(namespacesOfTypes.flatten())
            enrichedContext = enrichedContext.addUsedTypes(usedTypes.flatten().toSet())
            return ProcessorResult(emptyList(), enrichedContext)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Error processing variable declaration in ${context.fileInfo.physicalPath}:\n" +
                    "Node: ${rootNode.type}\n" +
                    "Content: ${nodeAsString(rootNode, context.source)}\n" +
                    "Error: ${e.message}",
                e
            )
        }
    }

    private fun determineUsage(node: TSNode): TypeOfUsage =
        if (node.type == "call_expression") TypeOfUsage.INSTANTIATION else TypeOfUsage.USAGE

    override fun appliesTo(node: TSNode) =
        node.type == "field_declaration" || node.type == "declaration" || node.type == "parameter_declaration"
}
