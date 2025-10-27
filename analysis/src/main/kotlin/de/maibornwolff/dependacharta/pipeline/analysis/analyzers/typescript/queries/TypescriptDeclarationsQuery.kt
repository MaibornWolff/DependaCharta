package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.model.Declaration
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterTypescript

/**
 *  [execute]
 */
class TypescriptDeclarationsQuery(
    val typescript: TreeSitterTypescript
) {
    private val unexportedDeclarationQuery = TSQuery(typescript, "(program (declaration) @declaration)")
    private val exportedDeclarationQuery = TSQuery(typescript, "(export_statement declaration: (declaration)) @exported_declaration")

    /**
     * Returns the typescript declarations contained within the given node.
     *
     * Example:
     * ```
     * export function myGreatFunction() {}
     * export const myGreatVariable = 42;
     * class MyGreatClass {}
     * ```
     * will return the following Declarations:
     * ```
     * Declaration(name="myGreatFunction", type=FUNCTION, node=<TSNode of the export statement>)
     * Declaration(name="myGreatVariable", type=VARIABLE, node=<TSNode of the export statement>)
     * Declaration(name="MyGreatClass", type=CLASS, node=<TSNode of the class declaration>)
     * ```.
     *
     * @param node the node to execute the query on
     * @return list of [TSNode]s representing the declarations
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ): List<Declaration> {
        val unexportedTopLevelDeclarations = node
            .execute(unexportedDeclarationQuery)
            .map { it.captures[0].node }
            .filter { it.type != "ambient_declaration" }
            .map { Declaration.fromUnexportedDeclaration(it, bodyContainingNode) }
        val exportedTopLevelDeclarations = node
            .execute(exportedDeclarationQuery)
            .map { it.captures[0].node }
            .filter { it.type != "ambient_declaration" }
            .map { Declaration.fromExportStatement(it, bodyContainingNode) }
        return unexportedTopLevelDeclarations + exportedTopLevelDeclarations
    }
}
