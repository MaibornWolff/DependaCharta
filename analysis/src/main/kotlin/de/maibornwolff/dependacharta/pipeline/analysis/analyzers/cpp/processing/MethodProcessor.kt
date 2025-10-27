package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.getChildren
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.CppQueryFactory
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.createNode
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.extractTypeWithFoundNamespacesAsDependencies
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import de.maibornwolff.codegraph.pipeline.analysis.model.*
import de.maibornwolff.codegraph.pipeline.shared.Logger
import org.treesitter.TSNode

class MethodProcessor : CppNodeProcessor {
    val functionDefinitionQueryString = "(function_definition) @body"

    data class MethodInfo(
        val returnType: TSNode?,
        val parameters: TSNode,
        val body: TSNode?,
        val name: TSNode
    )

    override fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult {
        try {
            var enrichedContext = context.copy()

            val methodInfo = getMethodInfo(rootNode, context)
            val className = getClassName(methodInfo.name, context) // TODO: bis hier reviewed.
            val (initializerTypes, initializerDependencies) = extractFromInitializer(rootNode, context)

            val (returnType, returnDependency) = extractFromReturn(methodInfo, context)
            enrichedContext = enrichedContext.addDependencies(returnDependency)

            val (paramTypes, paramDependencies) = extractFromParameters(methodInfo, context)
            val (usedTypesInBody, usedDependenciesInBody) = extractFromFunctionBody(methodInfo, context)

            val types = usedTypesInBody + returnType + initializerTypes + paramTypes
            enrichedContext = enrichedContext
                .addUsedTypes(
                    types.toSet()
                ).addDependencies(paramDependencies + initializerDependencies + usedDependenciesInBody)
            val foundNodes =
                if (className.isEmpty()) emptyList() else listOf(createNode(enrichedContext, className))
            return ProcessorResult(foundNodes, enrichedContext)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Error processing method in ${context.fileInfo.physicalPath}:\n" +
                    "Node: ${rootNode.type}\n" +
                    "Content: ${nodeAsString(rootNode, context.source)}\n" +
                    "Error: ${e.message}",
                e
            )
        }
    }

    private fun createNode(
        enrichedContext: CppContext,
        className: Path
    ): Node {
        val newNode = createNode(enrichedContext, className.getName(), NodeType.CLASS)
        if (className.hasOnlyName()) {
            return newNode
        }
        return newNode.copy(
            pathWithName = className
        )
    }

    private fun extractFromFunctionBody(
        functionInfo: MethodInfo,
        context: CppContext
    ): Pair<Set<Type>, Set<Dependency>> =
        functionInfo.body?.let { body ->
            val bodyProcessResult = BodyProcessor().process(context, body)
            bodyProcessResult.context.usedTypes to bodyProcessResult.context.getDependencies()
        } ?: (emptySet<Type>() to emptySet<Dependency>())

    private fun extractFromParameters(
        functionInfo: MethodInfo,
        context: CppContext
    ): Pair<List<Type>, Set<Dependency>> {
        val parametersAnalysisResult = VariableDeclarationProcessor().process(context, functionInfo.parameters)
        val paramTypes = parametersAnalysisResult.context.usedTypes.map {
            it.copy(usageSource = TypeOfUsage.ARGUMENT)
        }
        val paramDependencies = parametersAnalysisResult.context.getDependencies()
        return paramTypes to paramDependencies
    }

    private fun extractFromReturn(
        functionInfo: MethodInfo,
        context: CppContext,
    ): Pair<List<Type>, Set<Dependency>> {
        if (functionInfo.returnType == null) {
            return emptyList<Type>() to emptySet()
        }

        val (type, dependencies) = functionInfo.returnType.extractTypeWithFoundNamespacesAsDependencies(
            context.source,
            TypeOfUsage.RETURN_VALUE
        )
        return type to dependencies.toSet()
    }

    private fun extractFromInitializer(
        rootNode: TSNode,
        context: CppContext
    ): Pair<Set<Type>, Set<Dependency>> {
        val result = FieldInitializerProcessor().process(context, rootNode)
        return result.context.usedTypes to result.context.getDependencies()
    }

    private fun getClassName(
        nameNode: TSNode,
        context: CppContext
    ): Path {
        val childThatContainsRealNameInformation =
            nameNode.getChildren().firstOrNull { it.type == "qualified_identifier" }
        if (childThatContainsRealNameInformation != null) {
            val namespacePart = Path(nodeAsString(nameNode.getChildByFieldName("scope"), context.source))
            return namespacePart + getClassName(childThatContainsRealNameInformation, context)
        }
        if (nameNode.type == "qualified_identifier") {
            val name = nodeAsString(nameNode.getChildByFieldName("scope"), context.source)
            return Path(name)
        }

        return Path.empty()
    }

    private fun getMethodInfo(
        rootNode: TSNode,
        context: CppContext,
    ): MethodInfo {
        val functionDefinitionQuery = CppQueryFactory.getQuery(functionDefinitionQueryString)
        var functionInfo =
            rootNode
                .execute(functionDefinitionQuery)
                .map { getMethodInfo(it.captures[0].node) }
                .firstOrNull()
        if (functionInfo == null) {
            functionInfo = tryConstructor(rootNode)
            if (functionInfo == null) {
                // println is necessary at the moment to find function declarations, that can not be analyzed properly.
                Logger.d(
                    "---------------------------------------- ${context.fileInfo.physicalPath}  \n" +
                        nodeAsString(rootNode, context.source) + "\n"
                )

                throw IllegalStateException("Could not determine method information")
            }
        }

        return functionInfo
    }

    private fun getMethodInfo(rootNode: TSNode): MethodInfo? {
        val typeNode = tryFind(rootNode, "type")
        val declarator = findDeclaratorNode(rootNode)
        val name = tryFind(declarator, "declarator")
        val paramList = tryFind(declarator, "parameters")
        val body = tryFind(rootNode, "body")
        if (paramList != null && name != null) {
            return MethodInfo(typeNode, paramList, body, name)
        }
        return null
    }

    private fun findDeclaratorNode(node: TSNode): TSNode? {
        val result = tryFind(node, "declarator") ?: tryFindFunctionDeclarator(node)
        return if (result == null) {
            null
        } else if (tryFind(result, "parameters") != null) {
            result
        } else {
            findDeclaratorNode(result)
        }
    }

    private fun tryFindFunctionDeclarator(node: TSNode): TSNode? = node.getChildren().find { it.type == "function_declarator" }

    private fun tryFind(
        node: TSNode?,
        string: String
    ): TSNode? {
        return if (node == null) {
            null
        } else {
            val result = node.getChildByFieldName(string)
            return if (result.isNull) null else result
        }
    }

    private fun tryConstructor(tree: TSNode): MethodInfo? {
        val query = """
       (
          (function_definition
            declarator: (function_declarator
               declarator: (qualified_identifier) @function.identifier
               parameters: (parameter_list) @param_list)
            body: (compound_statement) @body)
        )
         (
          (function_definition
            declarator: (function_declarator
               declarator: (identifier) @function.identifier
               parameters: (parameter_list) @param_list)
            body: (compound_statement) @body)
        )
        """.trimIndent()

        val queryObj = CppQueryFactory.getQuery(query)

        return tree
            .execute(queryObj)
            .map {
                MethodInfo(null, it.captures[1].node, it.captures[2].node, it.captures[0].node)
            }.firstOrNull()
    }

    override fun appliesTo(node: TSNode) = node.type == "function_definition"
}
