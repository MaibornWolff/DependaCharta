package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing.BodyProcessor
import de.maibornwolff.codegraph.pipeline.analysis.model.FileInfo
import de.maibornwolff.codegraph.pipeline.analysis.model.FileReport
import de.maibornwolff.codegraph.pipeline.analysis.model.Node
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterCpp

class CppAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    private val cpp = TreeSitterCpp()

    override fun analyze(): FileReport {
        val rootNode = parseCode(fileInfo.content)
        val context = CppContext.empty(fileInfo)
        val allItems = processNode(context, rootNode)
        return FileReport(allItems)
    }

    private fun processNode(
        context: CppContext,
        rootNode: TSNode
    ): List<Node> {
        val processor = BodyProcessor()
        val processorResult = processor.process(context, rootNode)

        return processorResult.nodes
    }

    private fun parseCode(cppCode: String): TSNode {
        val parser = TSParser()
        parser.language = cpp
        val tree = parser.parseString(null, cppCode)
        return tree.rootNode
    }
}
