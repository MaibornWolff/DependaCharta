package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing

import org.treesitter.TSNode

class TypeDefProcessor :
    TypeDefAndAliasProcessor(
        """
    (type_definition 
                  type: (<TYPE>) @decl.type 
                  declarator: (type_identifier) @decl.name
                  )
        """.trimIndent(),
        0
    ) {
    override fun appliesTo(node: TSNode) = node.type == "type_definition"
}
