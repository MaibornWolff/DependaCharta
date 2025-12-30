package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.vue.queries

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.find
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import org.treesitter.TSLanguage
import org.treesitter.TSNode
import org.treesitter.TSQuery

data class ScriptBlock(
    val content: String,
    val lang: String?,
    val isSetup: Boolean
)

class VueScriptExtractorQuery(
    language: TSLanguage
) {
    private val scriptElementQuery = TSQuery(language, "(script_element) @script")

    fun execute(
        rootNode: TSNode,
        fileBody: String
    ): ScriptBlock? {
        val scriptMatches = rootNode.execute(scriptElementQuery)

        if (scriptMatches.isEmpty()) {
            return null
        }

        val scriptElement = scriptMatches
            .first()
            .captures
            .first()
            .node

        // Extract attributes
        val startTag = scriptElement.find("start_tag")
        var lang: String? = null
        var isSetup = false

        if (startTag != null) {
            val attributes = startTag.find("attribute")
            if (attributes != null) {
                val attrNameNode = attributes.find("attribute_name")
                val attrName = if (attrNameNode != null) nodeAsString(attrNameNode, fileBody) else null
                val attrValueNode = attributes.find("quoted_attribute_value")?.find("attribute_value")
                val attrValue = if (attrValueNode != null) nodeAsString(attrValueNode, fileBody) else null

                when (attrName) {
                    "lang" -> lang = attrValue
                    "setup" -> isSetup = true
                }
            }

            // Check for setup attribute
            for (i in 0 until startTag.childCount) {
                val child = startTag.getChild(i)
                if (child.type == "attribute") {
                    val nameNode = child.find("attribute_name")
                    val name = if (nameNode != null) nodeAsString(nameNode, fileBody) else null
                    if (name == "setup") {
                        isSetup = true
                    }
                    if (name == "lang") {
                        val valueNode = child.find("quoted_attribute_value")?.find("attribute_value")
                        val value = if (valueNode != null) nodeAsString(valueNode, fileBody) else null
                        lang = value
                    }
                }
            }
        }

        // Extract script content (raw_text node)
        val rawText = scriptElement.find("raw_text")
        val content = if (rawText != null) nodeAsString(rawText, fileBody) else ""

        return ScriptBlock(
            content = content.trim(),
            lang = lang,
            isSetup = isSetup
        )
    }
}
