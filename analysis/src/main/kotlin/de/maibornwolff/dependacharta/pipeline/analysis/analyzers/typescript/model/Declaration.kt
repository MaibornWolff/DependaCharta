package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript.model

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import org.treesitter.TSNode

data class Declaration(
    val name: String,
    val type: NodeType,
    val node: TSNode,
    val isAmbientModule: Boolean = false
) {
    companion object {
        fun fromExportStatement(
            exportStatementNode: TSNode,
            bodyContainingExportStatement: String
        ): Declaration {
            val declarationNode = exportStatementNode.getChildByFieldName("declaration")
            val declarationName = getNameFromDeclaration(declarationNode, bodyContainingExportStatement)
            return Declaration(
                name = declarationName,
                type = nodeType(declarationNode),
                node = exportStatementNode
            )
        }

        fun fromUnexportedDeclaration(
            declarationNode: TSNode,
            bodyContainingDeclaration: String
        ): Declaration {
            val declarationName = getNameFromDeclaration(declarationNode, bodyContainingDeclaration)
            return Declaration(
                name = declarationName,
                type = nodeType(declarationNode),
                node = declarationNode
            )
        }

        private fun getNameFromDeclaration(
            declarationNode: TSNode,
            bodyContainingDeclaration: String
        ): String {
            // lexical_declaration and variable_declaration are nested one level deeper
            val nameNode = if (declarationNode.type == "lexical_declaration" || declarationNode.type == "variable_declaration") {
                declarationNode.getNamedChild(0).getChildByFieldName("name")
            } else {
                declarationNode.getChildByFieldName("name")
            }
            return nodeAsString(nameNode, bodyContainingDeclaration)
        }

        private fun nodeType(it: TSNode) =
            when (it.type) {
                "function_declaration",
                "function_signature" -> NodeType.FUNCTION
                "class_declaration",
                "type_alias_declaration" -> NodeType.CLASS
                "interface_declaration" -> NodeType.INTERFACE
                "enum_declaration" -> NodeType.ENUM
                "variable_declaration",
                "lexical_declaration" -> NodeType.VARIABLE
                else -> NodeType.UNKNOWN
            }
    }
}
