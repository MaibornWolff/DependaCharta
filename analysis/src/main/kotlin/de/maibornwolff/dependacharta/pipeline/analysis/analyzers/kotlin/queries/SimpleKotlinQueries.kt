package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin.extractType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TSQueryMatch
import org.treesitter.TreeSitterKotlin

/**
 * [execute]
 */
class KotlinPropertyTypesQuery(
    val kotlin: TreeSitterKotlin
) {
    private val query = TSQuery(kotlin, "(property_declaration (variable_declaration) @var)")

    /**
     * Returns the types of properties declared within a node.
     *
     * Example:
     * ```
     * class MyClass {
     *     val myProperty: MyType = MyType()
     * }
     * ```
     * will return Types containing `MyType`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [Type] objects representing the types of the
     * properties declared within the node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ): List<Type> {
        return node
            .execute(query)
            .mapNotNull { extractTypeFromMatch(it, bodyContainingNode) }
    }
}

/**
 * [execute]
 */
class KotlinParameterTypesQuery(
    val kotlin: TreeSitterKotlin
) {
    private val parameterQuery = TSQuery(kotlin, "(parameter) @parameter")
    private val classParameterQuery = TSQuery(kotlin, "(class_parameter) @class_parameter")

    /**
     * Returns the types of parameters declared within a node.
     * Includes both function parameters and class constructor parameters.
     *
     * Example:
     * ```
     * class MyClass(val param: ParamType) {
     *     fun myFunction(arg: ArgType) {}
     * }
     * ```
     * will return Types containing `ParamType` and `ArgType`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [Type] objects representing the types of the
     * parameters declared within the node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ): List<Type> {
        val fromParameters = node
            .execute(parameterQuery)
            .mapNotNull { extractTypeFromMatch(it, bodyContainingNode) }
        val fromClassParameters = node
            .execute(classParameterQuery)
            .mapNotNull { extractTypeFromMatch(it, bodyContainingNode) }
        return fromParameters + fromClassParameters
    }
}

/**
 * [execute]
 */
class KotlinReturnTypesQuery(
    val kotlin: TreeSitterKotlin
) {
    private val query = TSQuery(kotlin, "(function_declaration) @function")

    /**
     * Returns the return types of functions declared within a node.
     *
     * Example:
     * ```
     * class MyClass {
     *     fun myFunction(): ReturnType {
     *         return ReturnType()
     *     }
     * }
     * ```
     * will return Types containing `ReturnType`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [Type] objects representing the return types of the
     * functions declared within the node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ): List<Type> {
        return node
            .execute(query)
            .mapNotNull { extractTypeFromMatch(it, bodyContainingNode) }
    }
}

/**
 * [execute]
 */
class KotlinAnnotationTypesQuery(
    val kotlin: TreeSitterKotlin
) {
    private val query = TSQuery(kotlin, "(annotation) @annotation")

    /**
     * Returns the annotations used within a node.
     *
     * Example:
     * ```
     * @MyAnnotation
     * class MyClass {}
     * ```
     * will return Types containing `MyAnnotation`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [Type] objects representing the types of the
     * annotations used within the node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ): List<Type> {
        return node
            .execute(query)
            .map { extractAnnotationType(it, bodyContainingNode) }
    }

    private fun extractAnnotationType(match: TSQueryMatch, nodeBody: String): Type {
        val capturedNode = match.captures[0].node
        val constructorInvocation = capturedNode.getNamedChild(0)
        if (constructorInvocation.isNull) return Type.unparsable()
        val typeNode = constructorInvocation.getNamedChild(0)
        if (typeNode.isNull) return Type.unparsable()
        return extractType(typeNode, nodeBody)
    }
}

/**
 * [execute]
 */
class KotlinConstructorCallQuery(
    val kotlin: TreeSitterKotlin
) {
    private val query = TSQuery(kotlin, "(call_expression) @call")

    /**
     * Returns the types of constructors called within a node.
     * Only uppercase types are returned (to filter out regular function calls).
     *
     * Example:
     * ```
     * class MyClass {
     *     fun myFunction() {
     *         val obj = MyType()
     *     }
     * }
     * ```
     * will return Types containing `MyType`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [Type] objects representing the types of the
     * constructors called within the node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ): List<Type> {
        return node
            .execute(query)
            .mapNotNull { extractSimpleType(it, bodyContainingNode) }
            .filter { it.isUppercase() }
    }
}

/**
 * [execute]
 */
class KotlinCallExpressionQuery(
    val kotlin: TreeSitterKotlin
) {
    private val query = TSQuery(kotlin, "(navigation_expression) @nav")

    /**
     * Returns the types of which methods or properties were accessed within a node.
     * Only uppercase types are returned.
     *
     * Example:
     * ```
     * class MyClass {
     *     val myVar = VarFactory.createVar() // method invocation
     *     val myEnum = MyEnum.ENUM_VALUE // property access
     *     val ignored = myVar.use(myEnum) // ignored, because myVar is not uppercase
     * }
     * ```
     * will return Types containing `VarFactory` and `MyEnum`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of uppercase [Type] objects representing the
     * types of which methods or properties were accessed within a node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ): List<Type> {
        return node
            .execute(query)
            .mapNotNull { extractSimpleType(it, bodyContainingNode) }
            .filter { it.isUppercase() }
    }
}

private fun extractTypeFromMatch(
    match: TSQueryMatch,
    nodeBody: String
): Type? {
    val node = match.captures[0].node
    val typeNode = node.getNamedChildren().find { it.type == "user_type" || it.type == "nullable_type" }
        ?: return null
    return extractType(typeNode, nodeBody)
}

private fun extractSimpleType(
    match: TSQueryMatch,
    nodeBody: String
): Type? {
    val capturedNode = match.captures[0].node
    val targetNode = capturedNode.getNamedChild(0)
    if (targetNode.isNull) return null
    return Type.simple(nodeAsString(targetNode, nodeBody))
}
