package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.java.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TSQueryMatch
import org.treesitter.TreeSitterJava

/**
 *  [execute]
 */
class JavaAnnotationTypesQuery(
    val java: TreeSitterJava
) {
    private val query = TSQuery(java, "[(annotation) (marker_annotation)] @annotation")

    /**
     * Returns the annotations used within a node.
     *
     * Example:
     * ```
     * @MyAnnotation
     * public class MyClass {}
     * ```
     * will return Types containing `MyAnnotation`
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [Type] objects representing the types of the
     * annotations used within the node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { extractType(it, bodyContainingNode, "name") }
}

/**
 *  [execute]
 */
class JavaFieldTypesQuery(
    val java: TreeSitterJava
) {
    private val query = TSQuery(java, "(field_declaration) @field")

    /**
     * Returns the types of fields within a node.
     *
     * Example:
     * ```
     * public class MyClass {
     *    private MyType myField;
     * }
     * ```
     * will return Types containing `MyType`
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [Type] objects representing the types of the
     * fields declared within the node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { extractType(it, bodyContainingNode, "type") }
}

/**
 *  [execute]
 */
class JavaVariableTypesQuery(
    val java: TreeSitterJava
) {
    private val query = TSQuery(java, "(local_variable_declaration) @variable")

    /**
     * Returns the types of variables declared within a node.
     *
     * Example:
     * ```
     * public class MyClass {
     *    private void myFunction() {
     *       MyType myVar = new MyType();
     *    }
     * }
     * ```
     * will return Types containing `MyType`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [Type] objects representing the types of the
     * variables declared within the node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { extractType(it, bodyContainingNode, "type") }
}

/**
 *  [execute]
 */
class JavaConstructorCallQuery(
    val java: TreeSitterJava
) {
    private val query = TSQuery(java, "(object_creation_expression) @creation")

    /**
     * Returns the types of constructors called within a node.
     *
     * Example:
     * ```
     * public class MyClass {
     *    private void myFunction() {
     *       new MyType();
     *    }
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
    ) = node
        .execute(query)
        .map { extractType(it, bodyContainingNode, "type") }
}

/**
 *  [execute]
 */
class JavaMethodIncovationsAndFieldAccessesQuery(
    val java: TreeSitterJava
) {
    private val query = TSQuery(java, "[(method_invocation) (field_access)] @invocation")

    /**
     * Returns the types of which methods or field were called within a node.
     * Only uppercase types are returned.
     *
     * Example:
     * ```
     * public class MyClass {
     *    private var myVar = VarFactory.createVar(); // method invocation
     *    private var myEnum = MyEnum.ENUM_VALUE; // field access
     *    private var ignored = myVar.use(myEnum); // ignored, because myVar is not uppercase
     * }
     * ```
     * will return Types containing `VarFactory` and `MyEnum`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of uppercase [Type] objects representing the
     * types of which methods or fields were accessed withing a node
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .map { extractType(it, bodyContainingNode, "object") }
        .filter { it.isUppercase() }
}

private fun extractType(
    match: TSQueryMatch,
    nodeBody: String,
    fieldName: String
): Type {
    val capturedNode = match.captures[0].node
    val typeNode = capturedNode.getChildByFieldName(fieldName)
    return de.maibornwolff.dependacharta.pipeline.analysis.analyzers.java
        .extractType(typeNode, nodeBody)
}
