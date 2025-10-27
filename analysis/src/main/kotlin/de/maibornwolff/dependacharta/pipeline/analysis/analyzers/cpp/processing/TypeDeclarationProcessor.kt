package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.CppQueryFactory
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.createNode
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.nodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import org.treesitter.TSNode
import org.treesitter.TSQueryMatch

class TypeDeclarationProcessor : CppNodeProcessor {
    val unionSpecifier = "union_specifier"
    val enumSpecifier = "enum_specifier"
    val structSpecifier = "struct_specifier"
    val classSpecifier = "class_specifier"
    val declarationsSpecifier = listOf(unionSpecifier, enumSpecifier, structSpecifier, classSpecifier)

    val typeDeclarationsQueryString =
        """
             [
  (union_specifier
    name: (type_identifier) @decl.name
    body: (field_declaration_list) @decl.body)
  @decl.type

(enum_specifier
    name: (type_identifier) @decl.name
    body: (enumerator_list) @decl.body)
  @decl.type

  (struct_specifier
    name: (type_identifier) @decl.name
    body: (field_declaration_list) @decl.body)
  @decl.type

  (class_specifier
    name: (type_identifier) @decl.name
    body: (field_declaration_list) @decl.body)
  @decl.type
  ]
"""

    override fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult {
        val typeDeclarationsQuery = CppQueryFactory.getQuery(typeDeclarationsQueryString)
        val (topLevelDeclarations, nestedDeclarations) = rootNode
            .execute(typeDeclarationsQuery)
            .filter { it.hasResult() }
            .partition {
                val parentType = it.captures[0]
                    .node.parent.type
                isTopLevelDeclarationType(parentType)
            }

        val topLevelNodes = topLevelDeclarations.flatMap {
            createNodes(
                context,
                it.captures[0].node,
                it.captures[1].node,
                it.captures[2].node,
                context.source,
                context.nameSpace
            )
        }

        val nestedNodes = nestedDeclarations.flatMap {
            createNodes(
                context,
                it.captures[0].node,
                it.captures[1].node,
                it.captures[2].node,
                context.source,
                context.nameSpace + getEnclosingName(it.captures[0].node, context.source)
            )
        }

        return ProcessorResult(topLevelNodes + nestedNodes, context)
    }

    private fun isTopLevelDeclarationType(parentType: String?): Boolean =
        parentType == "translation_unit" ||
            parentType == "namespace_definition" ||
            parentType == "declaration_list" ||
            parentType == "template_declaration"

    private fun getEnclosingName(
        node: TSNode,
        source: String
    ): String {
        var current = node.parent
        while (current?.type !in declarationsSpecifier && current != null) {
            current = current.parent
        }

        return current?.let { nodeAsString(it.getChildByFieldName("name"), source) } ?: ""
    }

    private fun createNodes(
        context: CppContext,
        typeNode: TSNode,
        nameNode: TSNode,
        bodyNode: TSNode,
        source: String,
        namespace: Path
    ): List<Node> {
        val name = nodeAsString(nameNode, source)
        val nodeType = nodeType(typeNode.type)
        val bodyResult = if (nodeType == NodeType.CLASS) {
            BodyProcessor().process(context.copy(nameSpace = namespace + Path(listOf(name))), bodyNode)
        } else {
            ProcessorResult(emptyList(), context)
        }

        return bodyResult.nodes + createNode(
            name,
            nodeType,
            bodyResult.context.usedTypes,
            bodyResult.context.getDependencies(),
            namespace,
            context.fileInfo.physicalPath
        )
    }

    override fun appliesTo(node: TSNode) =
        node.type == "union_specifier" ||
            node.type == "enum_specifier" ||
            node.type == "class_specifier" ||
            node.type == "struct_specifier"
}

private fun TSQueryMatch.hasResult() = this.captures.size > 0
