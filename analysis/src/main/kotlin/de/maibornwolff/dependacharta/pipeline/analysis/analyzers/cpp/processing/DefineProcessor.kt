package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import org.treesitter.TSNode

class DefineProcessor : CppNodeProcessor {
    override fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult = BodyProcessor().process(context, rootNode)

    override fun appliesTo(node: TSNode) = node.type == "preproc_ifdef" || node.type == "postproc_else"
}
