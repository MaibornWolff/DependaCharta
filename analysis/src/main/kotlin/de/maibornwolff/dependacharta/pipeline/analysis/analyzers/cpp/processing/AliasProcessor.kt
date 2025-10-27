package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing

import org.treesitter.TSNode

class AliasProcessor :
    TypeDefAndAliasProcessor(
        """
(alias_declaration 
  name: (type_identifier) @decl.name
  type: (type_descriptor type : (<TYPE>) @decl.type)  
)
    """,
        1
    ) {
    override fun appliesTo(node: TSNode) = node.type == "alias_declaration"
}
