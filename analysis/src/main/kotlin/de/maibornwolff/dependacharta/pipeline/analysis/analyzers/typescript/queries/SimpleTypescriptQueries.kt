package de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.queries

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterTypescript

/**
 *  [execute]
 */
class TypescriptTypeIdentifierQuery(
    val typescript: TreeSitterTypescript
) {
    private val query = TSQuery(typescript, "(type_identifier) @type")

    /**
     * Returns the type identifiers contained within the given node.
     *
     * Example:
     * ```
     * val myVar: MyType = 42;
     * ```
     * will result in the following type identifiers:
     * ```
     * listOf("MyType")
     * ```
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of used type identifiers
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { it.captures[0].node }
        .map { nodeAsString(it, bodyContainingNode) }
}

/**
 *  [execute]
 */
class TypescriptConstructorCallQuery(
    val typescript: TreeSitterTypescript
) {
    private val query = TSQuery(typescript, "(new_expression (identifier) @constructor)")

    /**
     * Returns the types of all called constructors within a node.
     *
     * Example:
     * ```
     * new MyType();
     * ```
     * will result in the following type identifiers:
     * ```
     * listOf("MyType")
     * ```
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of used constructor types
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { it.captures[0].node }
        .map { nodeAsString(it, bodyContainingNode) }
}

/**
 *  [execute]
 */
class TypescriptMemberExpressionQuery(
    val typescript: TreeSitterTypescript
) {
    private val query = TSQuery(typescript, "(member_expression (identifier) @memberExpression)")

    /**
     * Returns types whose members get accessed.
     *
     * Example:
     * ```
     * myVar.myProperty;
     * MyEnum.ENUM_VALUE
     * ```
     * will result in the following type identifiers:
     * ```
     * listOf("myVar", "MyEnum")
     * ```
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of used member types
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { it.captures[0].node }
        .map { nodeAsString(it, bodyContainingNode) }
}

/**
 *  [execute]
 */
class TypescriptExtendsClauseQuery(
    val typescript: TreeSitterTypescript
) {
    private val query = TSQuery(typescript, "(extends_clause (identifier) @extends)")

    /**
     * Returns the types that are extended within the given node.
     *
     * Example:
     * ```
     * class MyClass extends MySuperClass {}
     * ```
     * will result in the following type identifiers:
     * ```
     * listOf("MySuperClass")
     * ```
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of extended types
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { it.captures[0].node }
        .map { nodeAsString(it, bodyContainingNode) }
}

/**
 *  [execute]
 */
class TypescriptIdentifierQuery(
    val typescript: TreeSitterTypescript
) {
    private val query = TSQuery(typescript, "(identifier) @identifier")

    /**
     * Returns all identifiers used withing a given node.
     *
     * Example:
     * ```
     * constructor(id: CreatureId, type: CreatureType = SCT) {
     *    this.id = id;
     *    this.type = SCT;
     * }
     * ```
     * will result in the following type identifiers:
     * ```
     * listOf("id", "SCT")
     * ```
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of used identifiers
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { it.captures[0].node }
        .map { nodeAsString(it, bodyContainingNode) }
}

class TypescriptMethodCallIdentifierQuery(
    val typescript: TreeSitterTypescript
) {
    private val query = TSQuery(typescript, "(call_expression (identifier) @identifier)")

    /**
     * Returns the identifiers of called methods.
     *
     * Example:
     * ```
     * export function myCallingFunction() {
     *   myCalledFunction();
     * }
     * ```
     * will result in the following method call identifiers:
     * ```
     * listOf("myCalledFunction")
     * ```
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of used identifiers
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { it.captures[0].node }
        .map { nodeAsString(it, bodyContainingNode) }
}
