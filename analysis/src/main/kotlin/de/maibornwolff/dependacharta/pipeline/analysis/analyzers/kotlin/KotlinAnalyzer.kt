package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.find
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.getChildren
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin.queries.*
import de.maibornwolff.dependacharta.pipeline.analysis.model.*
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterKotlin

class KotlinAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    private val kotlin = TreeSitterKotlin()
    private val packageQuery = KotlinPackageQuery(kotlin)
    private val importQuery = KotlinImportQuery(kotlin)
    private val declarationsQuery = KotlinDeclarationsQuery(kotlin)
    private val inheritanceQuery = KotlinInheritanceQuery(kotlin)
    private val propertyTypesQuery = KotlinPropertyTypesQuery(kotlin)
    private val parameterTypesQuery = KotlinParameterTypesQuery(kotlin)
    private val returnTypesQuery = KotlinReturnTypesQuery(kotlin)
    private val annotationTypesQuery = KotlinAnnotationTypesQuery(kotlin)
    private val constructorCallQuery = KotlinConstructorCallQuery(kotlin)
    private val callExpressionQuery = KotlinCallExpressionQuery(kotlin)
    private val bareTypeReferenceQuery = KotlinBareTypeReferenceQuery(kotlin)
    private val callableReferenceQuery = KotlinCallableReferenceQuery(kotlin)

    override fun analyze(): FileReport {
        val rootNode = parseCode(fileInfo.content)
        val packageResult = packageQuery.execute(rootNode, fileInfo.content)
        val dependencies = importQuery.execute(rootNode, fileInfo.content)
        val declarations = declarationsQuery.execute(rootNode)
            .filter { isTopLevelDeclaration(it) }

        // Build a map of byte offset -> declaration name for parent lookup
        val declarationsByOffset = declarations.associateBy(
            { it.startByte },
            { extractDeclarationName(it) }
        )

        val nodes = declarations.map { declaration ->
            val parentPath = findParentClassPath(declaration, declarationsByOffset)
            extractNodeFromDeclaration(
                packageResult,
                parentPath,
                dependencies,
                declaration
            )
        }
        return FileReport(nodes)
    }

    private fun extractDeclarationName(declaration: TSNode): String {
        val nodeBody = nodeAsString(declaration, fileInfo.content)
        val declarationNode = parseCode(nodeBody).getChild(0)
        val nameNode = declarationNode.find("type_identifier") ?: declarationNode.find("simple_identifier")
        return nameNode?.let { nodeAsString(it, nodeBody) } ?: "Unknown"
    }

    private fun findParentClassPath(
        declaration: TSNode,
        declarationsByOffset: Map<Int, String>
    ): List<String> {
        val parentClasses = mutableListOf<String>()
        var current = declaration.parent

        while (!current.isNull) {
            if (current.type == "class_declaration" || current.type == "object_declaration") {
                // Check if this parent is in our declarations map
                val parentName = declarationsByOffset[current.startByte]
                if (parentName != null) {
                    parentClasses.add(0, parentName)
                }
            }
            current = current.parent
        }

        return parentClasses
    }

    private fun extractNodeFromDeclaration(
        packagePath: List<String>,
        parentClassPath: List<String>,
        imports: List<Dependency>,
        declaration: TSNode
    ): Node {
        val nodeBody = nodeAsString(declaration, fileInfo.content)
        val declarationNode = parseCode(nodeBody).getChild(0)
        val nameNode = declarationNode.find("type_identifier") ?: declarationNode.find("simple_identifier")
        val declarationName = nameNode?.let { nodeAsString(it, nodeBody) } ?: "Unknown"

        val implicitWildcardImport = Dependency(path = Path(packagePath), isWildcard = true)
        val usedTypes = extractUsedTypes(declarationNode, nodeBody)

        // Full path includes package + parent classes + declaration name
        val fullPath = packagePath + parentClassPath + declarationName

        return Node(
            pathWithName = Path(fullPath),
            physicalPath = fileInfo.physicalPath,
            language = SupportedLanguage.KOTLIN,
            nodeType = nodeType(declarationNode),
            dependencies = (imports + implicitWildcardImport).toSet(),
            usedTypes = usedTypes
        )
    }

    private fun parseCode(kotlinCode: String): TSNode {
        val parser = TSParser()
        parser.language = kotlin
        val tree = parser.parseString(null, kotlinCode)
        return tree.rootNode
    }

    private fun extractUsedTypes(
        declaration: TSNode,
        nodeBody: String,
    ): Set<Type> {
        val inheritanceTypes = inheritanceQuery.execute(declaration, nodeBody)
        val propertyTypes = propertyTypesQuery.execute(declaration, nodeBody)
        val parameterTypes = parameterTypesQuery.execute(declaration, nodeBody)
        val returnTypes = returnTypesQuery.execute(declaration, nodeBody)
        val annotations = annotationTypesQuery.execute(declaration, nodeBody)
        val constructorCalls = constructorCallQuery.execute(declaration, nodeBody)
        val callExpressions = callExpressionQuery.execute(declaration, nodeBody)
        val bareTypeReferences = bareTypeReferenceQuery.execute(declaration, nodeBody)
        val callableReferences = callableReferenceQuery.execute(declaration, nodeBody)
        return (
            inheritanceTypes + propertyTypes + parameterTypes + returnTypes +
                annotations + constructorCalls + callExpressions + bareTypeReferences +
                callableReferences
        ).toSet()
    }

    private fun nodeType(declaration: TSNode): NodeType {
        if (declaration.type == "object_declaration") return NodeType.CLASS
        if (declaration.type == "function_declaration") return NodeType.FUNCTION
        if (declaration.type != "class_declaration") return NodeType.UNKNOWN

        val children = declaration.getChildren()
        return when {
            children.any { it.type == "interface" } -> NodeType.INTERFACE
            children.any { it.type == "enum" } -> NodeType.ENUM
            else -> NodeType.CLASS
        }
    }

    private fun isTopLevelDeclaration(node: TSNode): Boolean {
        if (node.type != "function_declaration") return true

        var current = node.parent
        while (!current.isNull) {
            when (current.type) {
                "class_declaration", "object_declaration", "function_declaration" -> return false
                "source_file" -> return true
            }
            current = current.parent
        }
        return true
    }
}
