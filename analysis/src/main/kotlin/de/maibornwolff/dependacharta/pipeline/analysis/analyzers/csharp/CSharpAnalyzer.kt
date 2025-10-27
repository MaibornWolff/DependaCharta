package de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.model.UsingDirective
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.queries.*
import de.maibornwolff.codegraph.pipeline.analysis.model.*
import de.maibornwolff.codegraph.pipeline.shared.SupportedLanguage
import org.treesitter.TSNode
import org.treesitter.TSParser

class CSharpAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    private val fileScopedNamespaceQuery = CSharpFileScopedNamespaceQuery()
    private val topLevelDeclarationsQuery = CSharpDeclarationsQuery()
    private val constructorParameterQuery = CSharpConstructorParameterQuery()
    private val methodTypesQuery = CSharpMethodTypesQuery()
    private val inheritanceTypesQuery = CSharpInheritanceTypesQuery()
    private val variableDeclarationQuery = CSharpVariableDeclarationQuery()
    private val objectCreationQuery = CSharpObjectCreationQuery()
    private val memberAccessQuery = CSharpMemberAccessesQuery()
    private val attributeQuery = CSharpAttributeQuery()
    private val namespaceQuery = CSharpNamespaceQuery()
    private val castQuery = CSharpCastQuery()
    private val genericParameterQuery = CSharpGenericParameterQuery()
    private val genericTypeConstraintQuery = CSharpGenericTypeConstraintQuery()
    private val isTypeCheckQuery = CSharpIsTypeCheckingQuery()

    override fun analyze(): FileReport {
        val rootNode = parseCode(fileInfo.content)

        val fileScopedNamespace = fileScopedNamespaceQuery.execute(rootNode, fileInfo.content)
        val globalUsingDirectives = findUsingDirectives(rootNode, fileInfo.content)
        val topLevelDeclarations = topLevelDeclarationsQuery.execute(rootNode)

        val topLevelNodes = topLevelDeclarations.map {
            analyzeDeclaration(it, fileInfo.content, fileScopedNamespace ?: listOf(), globalUsingDirectives)
        }
        val nodesWithinNamespaces = analyzeNamespaceDeclarations(rootNode, fileInfo.content, globalUsingDirectives)
        return FileReport(topLevelNodes + nodesWithinNamespaces)
    }

    private fun findUsingDirectives(
        node: TSNode,
        nodeBody: String
    ): List<UsingDirective> =
        node
            .getNamedChildren()
            .filter {
                it.type == "using_directive"
            }.map {
                val usingNode = it
                val aliasNode = usingNode.getChildByFieldName("name")
                val alias = if (aliasNode.isNull) null else nodeAsString(aliasNode, nodeBody)
                val namespaceIndex = if (alias != null) 1 else 0
                val namespaceNode = usingNode.getNamedChild(namespaceIndex)
                UsingDirective(nodeAsString(namespaceNode, nodeBody).split("."), alias)
            }

    private fun analyzeNamespaceDeclarations(
        node: TSNode,
        nodeBody: String,
        directives: List<UsingDirective>
    ): List<Node> {
        val namespaces = namespaceQuery.execute(node)
        return namespaces.flatMap {
            val namespace = nodeAsString(it.getNamedChild(0), nodeBody).split(".")
            analyzeNamespace(it, nodeBody, namespace, directives)
        }
    }

    private fun analyzeNamespace(
        node: TSNode,
        content: String,
        namespace: List<String>,
        directives: List<UsingDirective>
    ): List<Node> {
        val namespaceBody = node.getChildByFieldName("body")
        val declarations = namespaceBody.getNamedChildren().filter { isDeclaration(it.type) }
        val namespaceDirectives = findUsingDirectives(namespaceBody, content)
        return declarations.map {
            analyzeDeclaration(it, content, namespace, directives + namespaceDirectives)
        }
    }

    private fun isDeclaration(type: String): Boolean =
        type in listOf(
            "class_declaration",
            "struct_declaration",
            "record_declaration",
            "interface_declaration",
            "enum_declaration",
            "delegate_declaration"
        )

    private fun analyzeDeclaration(
        declaration: TSNode,
        fileContent: String,
        namespace: List<String>,
        directives: List<UsingDirective>
    ): Node {
        val name = nodeAsString(declaration.getChildByFieldName("name"), fileContent)
        val declarationString = nodeAsString(declaration, fileContent)
        val node = parseCode(declarationString)

        val dependencies = directives.map { Dependency(path = Path(it.path), isWildcard = true) }.toSet()
        val implicitWildcard = Dependency(path = Path(namespace), isWildcard = true)
        val usedTypes = extractUsedTypes(node, declarationString)
        return Node(
            pathWithName = Path(namespace + name),
            physicalPath = fileInfo.physicalPath,
            language = SupportedLanguage.C_SHARP,
            nodeType = nodeType(declaration.type),
            dependencies = (dependencies + implicitWildcard),
            usedTypes = usedTypes.toSet()
        )
    }

    private fun extractUsedTypes(
        node: TSNode,
        declarationString: String,
    ): List<Type> {
        val constructorTypes = constructorParameterQuery.execute(node, declarationString)
        val methodTypes = methodTypesQuery.execute(node, declarationString)
        val castTypes = castQuery.execute(node, declarationString)
        val genericTypeParameters = genericParameterQuery.execute(node, declarationString)
        val genericTypeConstraints = genericTypeConstraintQuery.execute(node, declarationString)
        val inheritedTypes = inheritanceTypesQuery.execute(node, declarationString)
        val variableTypes = variableDeclarationQuery.execute(node, declarationString)
        val objectCreations = objectCreationQuery.execute(node, declarationString)
        val memberAccesses = memberAccessQuery.execute(node, declarationString)
        val attributes = handleAttributeSuffix(attributeQuery.execute(node, declarationString))
        val isTypeChecks = isTypeCheckQuery.execute(node, declarationString)
        return constructorTypes + methodTypes + castTypes +
            genericTypeParameters + genericTypeConstraints + inheritedTypes + variableTypes +
            objectCreations + memberAccesses + attributes + isTypeChecks
    }

    private fun handleAttributeSuffix(attributes: List<Type>) = attributes + attributes.map { Type.simple(it.name + "Attribute") }

    private fun nodeType(type: String?): NodeType =
        when (type) {
            "struct_declaration",
            "record_declaration",
            "class_declaration" -> NodeType.CLASS
            "delegate_declaration",
            "interface_declaration" -> NodeType.INTERFACE
            "enum_declaration" -> NodeType.ENUM
            else -> NodeType.UNKNOWN
        }

    private fun parseCode(cSharpCode: String): TSNode {
        val parser = TSParser()
        parser.language = CSharpQueryFactory.CsharpLanguage
        val tree = parser.parseString(null, cSharpCode)
        return tree.rootNode
    }
}
