package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.model.toImport
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.CppQueryFactory
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.Include
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import de.maibornwolff.codegraph.pipeline.analysis.model.Path
import org.treesitter.TSNode

class IncludeProcessor : CppNodeProcessor {
    override fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult {
        val includeQuery = CppQueryFactory.getQuery(
            """
            [
            (preproc_include path: (system_lib_string) @lib)
            (preproc_include path:(string_literal(string_content)@include)@all (#not-match? @all "\\\\\r?\n"))  
            ]
            """.trimIndent()
        )

        val includes = rootNode
            .execute(includeQuery)
            .map {
                Include(
                    extractName(it.captures[0].node, context.source).toImport(),
                    Path.fromPhysicalPath(context.fileInfo.physicalPath)
                )
            }

        val enrichedContext = context.addIncludes(includes)

        return ProcessorResult(emptyList(), enrichedContext)
    }

    private fun removeDelimiter(text: String): String = text.replace("<", "").replace(">", "")

    private fun removeParenthesis(text: String): String = text.replace("\"", "")

    private fun removeNewLine(text: String): String = text.replace("\n", "").replace("\r", "")

    private fun removeSlash(text: String): String = text.replace("\\", "")

    private fun removeSpaces(text: String): String = text.replace(" ", "")

    private fun extractName(
        node: TSNode,
        bodyContainingNode: String
    ): String =
        nodeAsString(node, bodyContainingNode)
            .let(::removeDelimiter)
            .let(::removeParenthesis)
            .let(::removeNewLine)
            .let(::removeSlash)
            .let(::removeSpaces)

    override fun appliesTo(node: TSNode) = node.type == "preproc_include"
}
