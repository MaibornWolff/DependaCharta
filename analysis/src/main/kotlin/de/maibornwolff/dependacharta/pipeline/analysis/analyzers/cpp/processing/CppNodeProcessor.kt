package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import org.treesitter.TSNode

interface CppNodeProcessor {
    fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult

    fun appliesTo(node: TSNode): Boolean
}
