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

    override fun analyze(): FileReport {
        val rootNode = parseCode(fileInfo.content)
        val packageResult = packageQuery.execute(rootNode, fileInfo.content)
        val dependencies = importQuery.execute(rootNode, fileInfo.content)
        val declarations = declarationsQuery.execute(rootNode)
        val nodes = declarations.map {
            extractNodeFromDeclaration(
                packageResult,
                dependencies,
                it
            )
        }
        return FileReport(nodes)
    }

    private fun extractNodeFromDeclaration(
        packagePath: List<String>,
        imports: List<Dependency>,
        declaration: TSNode
    ): Node {
        val nodeBody = nodeAsString(declaration, fileInfo.content)
        val declarationNode = parseCode(nodeBody).getChild(0)
        val nameNode = declarationNode.find("type_identifier") ?: declarationNode.find("simple_identifier")
        val declarationName = nameNode?.let { nodeAsString(it, nodeBody) } ?: "Unknown"

        val implicitWildcardImport = Dependency(path = Path(packagePath), isWildcard = true)
        val usedTypes = extractUsedTypes(declarationNode, nodeBody)
        return Node(
            pathWithName = Path(packagePath + declarationName),
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
        return (
            inheritanceTypes + propertyTypes + parameterTypes + returnTypes +
                annotations + constructorCalls + callExpressions
        ).toSet()
    }

    private fun nodeType(declaration: TSNode): NodeType {
        if (declaration.type == "object_declaration") return NodeType.CLASS
        if (declaration.type != "class_declaration") return NodeType.UNKNOWN

        val children = declaration.getChildren()
        return when {
            children.any { it.type == "interface" } -> NodeType.INTERFACE
            children.any { it.type == "enum" } -> NodeType.ENUM
            else -> NodeType.CLASS
        }
    }
}
