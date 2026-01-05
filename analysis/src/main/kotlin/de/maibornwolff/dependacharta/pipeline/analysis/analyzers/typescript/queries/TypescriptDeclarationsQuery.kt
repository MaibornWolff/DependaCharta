package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.find
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.model.Declaration
import org.treesitter.TSLanguage
import org.treesitter.TSNode
import org.treesitter.TSQuery

/**
 *  [execute]
 */
class TypescriptDeclarationsQuery(
    val language: TSLanguage
) {
    private val unexportedDeclarationQuery = TSQuery(language, "(program (declaration) @declaration)")
    private val ambientDeclarationQuery = TSQuery(
        language,
        "(program (ambient_declaration) @ambient_declaration)"
    )
    private val exportedDeclarationQuery = TSQuery(
        language,
        "(program (export_statement declaration: (declaration)) @exported_declaration)"
    )
    private val moduleExportedDeclarationQuery = TSQuery(
        language,
        "(export_statement declaration: (declaration)) @exported_declaration"
    )

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
            .filter { isKnownDeclarationType(it.type) }
            .map { Declaration.fromUnexportedDeclaration(it, bodyContainingNode) }

        val exportedTopLevelDeclarations = node
            .execute(exportedDeclarationQuery)
            .map { it.captures[0].node }
            .map { Declaration.fromExportStatement(it, bodyContainingNode) }

        val ambientModuleDeclarations = node
            .execute(ambientDeclarationQuery)
            .map { it.captures[0].node }
            .flatMap { extractDeclarationsFromAmbientModule(it, bodyContainingNode) }

        return unexportedTopLevelDeclarations + exportedTopLevelDeclarations + ambientModuleDeclarations
    }

    private fun isKnownDeclarationType(type: String): Boolean {
        return type in listOf(
            "function_declaration",
            "class_declaration",
            "type_alias_declaration",
            "interface_declaration",
            "enum_declaration",
            "variable_declaration",
            "lexical_declaration"
        )
    }

    private fun extractDeclarationsFromAmbientModule(
        ambientNode: TSNode,
        bodyContainingNode: String
    ): List<Declaration> {
        // ambient_declaration contains a 'module' child
        val moduleNode = ambientNode.find("module") ?: return emptyList()

        val moduleName = extractModuleName(moduleNode, bodyContainingNode) ?: return emptyList()

        // Skip wildcard modules like "*.md"
        if (moduleName.contains("*")) {
            return emptyList()
        }

        // Find the module body inside the module node
        val moduleBody = findModuleBody(moduleNode) ?: return emptyList()

        return moduleBody
            .execute(moduleExportedDeclarationQuery)
            .map { it.captures[0].node }
            .map { exportNode ->
                val declaration = Declaration.fromExportStatement(exportNode, bodyContainingNode)
                declaration.copy(
                    name = "$moduleName/${declaration.name}",
                    isAmbientModule = true
                )
            }
    }

    private fun findModuleBody(moduleNode: TSNode): TSNode? {
        // The module body could be statement_block or similar
        return moduleNode.find("statement_block")
            ?: moduleNode.find("module_body")
            ?: moduleNode.getNamedChildren().find { it.type.contains("block") || it.type.contains("body") }
    }

    private fun extractModuleName(
        ambientNode: TSNode,
        bodyContainingNode: String
    ): String? {
        val stringNode = ambientNode.find("string") ?: return null
        val rawName = nodeAsString(stringNode, bodyContainingNode)
        return rawName.trim('"', '\'')
    }
}
