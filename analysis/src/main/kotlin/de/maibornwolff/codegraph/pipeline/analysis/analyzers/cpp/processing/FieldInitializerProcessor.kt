package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.getChildren
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.CppQueryFactory
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.extractTypeWithFoundNamespacesAsDependencies
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency
import de.maibornwolff.codegraph.pipeline.analysis.model.Type
import de.maibornwolff.codegraph.pipeline.analysis.model.TypeOfUsage
import org.treesitter.TSNode

class FieldInitializerProcessor : CppNodeProcessor {
    val queryInitializerWithCallOrQualifiedIdentifier =
        "(field_initializer_list (field_initializer (initializer_list [(call_expression) (qualified_identifier)]@initializer)))"
    val queryManyFieldInitializer = "(field_initializer_list (field_initializer (argument_list)@initializer))"

    override fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult {
        val (usedTypes, dependencies) = extractFromCallOrQualifiedIdentifier(rootNode, context)
        val (usedTypesOfFieldInitializers, dependencyOfFieldInitializers) = extractFromManyFieldInitializers(rootNode, context)

        return ProcessorResult(
            emptyList(),
            context
                .copy()
                .addDependencies(
                    dependencyOfFieldInitializers + dependencies
                ).addUsedTypes(usedTypesOfFieldInitializers + usedTypes)
        )
    }

    private fun extractFromManyFieldInitializers(
        node: TSNode,
        context: CppContext
    ): Pair<Set<Type>, Set<Dependency>> {
        val query = CppQueryFactory.getQuery(queryManyFieldInitializer)
        val initializerNodes = node.execute(query)

        if (initializerNodes.isEmpty()) {
            return emptySet<Type>() to emptySet()
        }

        val arguments = initializerNodes
            .map { it.captures[0].node }
            .filter { it.type == "argument_list" }
            .flatMap { node ->
                node.getNamedChildren()
            }

        val (types, dependencies) = arguments
            .filter { it.type == "qualified_identifier" }
            .map { it.extractTypesAndDependenciesFromInitializer(context.source, TypeOfUsage.INSTANTIATION) }
            .unzip()

        val (typesOfCalls, dependenciesOfCalls) = arguments
            .filter { it.type == "call_expression" }
            .flatMap { it.getChildren() }
            .filter { it.type == "qualified_identifier" }
            .map { it.extractTypesAndDependenciesFromInitializer(context.source, TypeOfUsage.INSTANTIATION) }
            .unzip()

        return types.toSet() + typesOfCalls.toSet() to dependencies.toSet() + dependenciesOfCalls.toSet()
    }

    private fun extractFromCallOrQualifiedIdentifier(
        node: TSNode,
        context: CppContext
    ): Pair<Set<Type>, Set<Dependency>> {
        val query = CppQueryFactory.getQuery(queryInitializerWithCallOrQualifiedIdentifier)
        val initializerNodes = node.execute(query)

        if (initializerNodes.isEmpty()) {
            return emptySet<Type>() to emptySet()
        }

        val (types, dependencies) = initializerNodes
            .map { it.captures[0].node }
            .map { node ->
                val (type, dependencies) = if (node.type == "call_expression") {
                    node.extractDependenciesFromFunctionCall(context)
                } else {
                    node.extractTypesAndDependenciesFromInitializer(context.source, TypeOfUsage.INSTANTIATION)
                }
                type to dependencies
            }.unzip()

        return types.toSet() to dependencies.toSet()
    }

    override fun appliesTo(node: TSNode): Boolean = node.type == "field_initializer_list"
}

private fun TSNode.extractDependenciesFromFunctionCall(context: CppContext): Pair<Type, Dependency> {
    val namespaceNode =
        this
            .getChildren()
            .first()
            .getChildren()
            .first { it.type == "namespace_identifier" }
    val namespace = nodeAsString(namespaceNode, context.source)
    return Type.unparsable() to Dependency.asWildcard(namespace)
}

private fun TSNode.extractTypesAndDependenciesFromInitializer(
    nodeBody: String,
    typeOfUsage: TypeOfUsage = TypeOfUsage.USAGE
): Pair<Type, Dependency> {
    val (_, namespacesAsDependencies) = this.extractTypeWithFoundNamespacesAsDependencies(nodeBody, typeOfUsage)

    val trueType = Type.simple(namespacesAsDependencies.map { it.path.parts.last() }.last())
    val trueNamespace = Dependency.asWildcard(namespacesAsDependencies.flatMap { it.path.parts.dropLast(1) })

    return trueType to trueNamespace
}
