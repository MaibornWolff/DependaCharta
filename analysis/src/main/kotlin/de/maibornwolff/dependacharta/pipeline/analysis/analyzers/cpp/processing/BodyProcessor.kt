package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.processing

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model.ProcessorResult
import de.maibornwolff.codegraph.pipeline.analysis.common.splitNameToParts
import de.maibornwolff.codegraph.pipeline.analysis.model.Node
import de.maibornwolff.codegraph.pipeline.analysis.model.Path
import org.treesitter.TSNode

class BodyProcessor : CppNodeProcessor {
    private val nodeProcessors = listOf<CppNodeProcessor>(
        CommentProcessor(),
        UsingProcessor(),
        IncludeProcessor(),
        NamespaceProcessor(),
        TypeDeclarationProcessor(),
        InheritanceProcessor(),
        MethodProcessor(),
        TypeDefProcessor(),
        AliasProcessor(),
        GenericTemplateProcessor(),
        DefineProcessor()
    )

    /*
    TODO: Found smells that should be addressed:
        - the context argument and the underlying CppContext should not be necessary to pass on to all processors.
        It shouldn't be necessary to have a state while analyzing the different structures of a cpp file.
        - we regard the duplication of the context to achieve a scoped context a smell as well.
        - the processors are doing to much work. body processor is called for every "code body block" that is found.
        - check todos in TypeExtractionService. those are smells too.
        - applicable node types like "enum_specifier" should be extracted
        - logic in `appliesTo` should check against processor-specific list of node types
        - rename `appliesTo` to `isApplicable`
     */
    override fun process(
        context: CppContext,
        rootNode: TSNode
    ): ProcessorResult {
        var result = mutableListOf<Node>()
        var scopedContext = context.copy()
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            if (child.isNamed) {
                val processors = getProcessors(child)
                for (processor in processors) {
                    val processorResult = processor.process(scopedContext, child)
                    result.addAll(processorResult.nodes)
                    result = result.addTypesAndDependenciesToRelatedNode(processorResult.context)
                    scopedContext = processorResult.context
                }
            }
        }
        val (nonFullyQualifiedNodes, fullyQualifiedNodes) = result.partition { it.pathWithName.hasOnlyName() }
        val nodes = nonFullyQualifiedNodes.map(::fullyQualify) + fullyQualifiedNodes
        val consolidatedNodes = nodes.groupBy { it.pathWithName }.map { consolidate(it.value) }
        return ProcessorResult(consolidatedNodes, scopedContext)
    }

    private fun fullyQualify(node: Node): Node {
        val path = Path(splitNameToParts(node.physicalPath).filter { it != "." }) + node.pathWithName
        return node.copy(pathWithName = path)
    }

    private fun consolidate(nodes: List<Node>): Node {
        val reducedNode = nodes.reduce { a, b ->
            a.copy(
                dependencies = a.dependencies + b.dependencies,
                usedTypes = a.usedTypes + b.usedTypes
            )
        }
        return reducedNode
    }

    private fun getProcessors(node: TSNode): List<CppNodeProcessor> {
        var applicableProcessors = nodeProcessors.filter { it.appliesTo(node) }
        if (applicableProcessors.isEmpty()) {
            applicableProcessors = listOf(VariableDeclarationProcessor())
        }
        return applicableProcessors
    }

    override fun appliesTo(node: TSNode) = false
}

private fun MutableList<Node>.addTypesAndDependenciesToRelatedNode(processorResult: CppContext): MutableList<Node> {
    val relatedNode = this.lastOrNull() ?: return this
    val nodes = this.dropLast(1).toMutableList()
    val enrichedNode = relatedNode.copy(
        dependencies = relatedNode.dependencies + processorResult.getDependencies(),
        usedTypes = relatedNode.usedTypes + processorResult.usedTypes
    )
    nodes.add(enrichedNode)
    return nodes
}
