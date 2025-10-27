package de.maibornwolff.dependacharta.pipeline.analysis.contract

import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage

class JavaAnalysisPipelineTests : AnalysisPipelineContractTests() {
    override val language: SupportedLanguage = SupportedLanguage.JAVA
    override val rootDirectory: String = "src/test/resources/analysis/contract/examples/java"
    override val nodesBuilder: ExpectedNodesBuilder = SotsJavaExpectedNodesBuilder()
}

class SotsJavaExpectedNodesBuilder : SotsExpectedNodesBuilder() {
    override fun build(): List<ExpectedNode> {
        val expectedNodes = super.build()
        return expectedNodes.map { expectedNode ->
            expectedNode.copy(
                ignorableDependencies = setOf(),
                ignorableTypes = setOf(
                    Type.Companion.simple("Map"),
                    Type.Companion.simple("List"),
                    Type.Companion.simple("void"),
                    Type.Companion.simple("int"),
                    Type.Companion.simple("UUID"),
                    Type.Companion.simple("String"),
                    Type.Companion.simple("Target"),
                    Type.Companion.simple("ElementType"),
                )
            )
        }
    }

    override fun buildFightableResult(): ExpectedNode {
        val result = super.buildFightableResult()
        val node = result.nodes.first()
        return result.copy(
            nodes = listOf(
                node.copy(
                    nodeType = NodeType.ANNOTATION
                )
            )
        )
    }
}
