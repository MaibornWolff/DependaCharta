package de.maibornwolff.codegraph.pipeline.analysis.analyzers.java

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.java.queries.*
import de.maibornwolff.codegraph.pipeline.analysis.model.*
import de.maibornwolff.codegraph.pipeline.shared.SupportedLanguage
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterJava

class JavaAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    private val java = TreeSitterJava()
    private val methodAndConstructorQuery = JavaMethodAndConstructorQuery(java)
    private val inheritanceQuery = JavaInheritanceQuery(java)
    private val annotationTypesQuery = JavaAnnotationTypesQuery(java)
    private val fieldTypesQuery = JavaFieldTypesQuery(java)
    private val variableTypesQuery = JavaVariableTypesQuery(java)
    private val constructorCallQuery = JavaConstructorCallQuery(java)
    private val methodIncovationsAndFieldAccessesQuery = JavaMethodIncovationsAndFieldAccessesQuery(java)
    private val thrownTypesQuery = JavaThrownTypesQuery(java)
    private val declarationsQuery = JavaDeclarationsQuery(java)
    private val packageQuery = JavaPackageQuery(java)
    private val importQuery = JavaImportQuery(java)

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
        val declarationName = nodeAsString(declarationNode.getChildByFieldName("name"), nodeBody)

        val implicitWildcardImport = Dependency(path = Path(packagePath), isWildcard = true)
        val usedTypes = extractUsedTypes(declarationNode, nodeBody)
        return Node(
            pathWithName = Path(packagePath + declarationName),
            physicalPath = fileInfo.physicalPath,
            language = SupportedLanguage.JAVA,
            nodeType = nodeType(declarationNode),
            dependencies = (imports + implicitWildcardImport).toSet(),
            usedTypes = usedTypes
        )
    }

    private fun parseCode(javaCode: String): TSNode {
        val parser = TSParser()
        parser.language = java
        val tree = parser.parseString(null, javaCode)
        return tree.rootNode
    }

    private fun extractUsedTypes(
        declaration: TSNode,
        nodeBody: String,
    ): Set<Type> {
        val interfaceImplementations = inheritanceQuery.execute(declaration, nodeBody)
        val variables = variableTypesQuery.execute(declaration, nodeBody)
        val annotations = annotationTypesQuery.execute(declaration, nodeBody)
        val methodInvocationsAndFieldAccesses = methodIncovationsAndFieldAccessesQuery.execute(declaration, nodeBody)
        val objectCreations = constructorCallQuery.execute(declaration, nodeBody)
        val thrownTypes = thrownTypesQuery.execute(declaration, nodeBody)
        val methodTypes = methodAndConstructorQuery.execute(declaration, nodeBody)
        val fieldTypes = fieldTypesQuery.execute(declaration, nodeBody)
        return (
            interfaceImplementations + variables + annotations + methodInvocationsAndFieldAccesses +
                objectCreations + thrownTypes + fieldTypes + methodTypes
        ).toSet()
    }

    private fun nodeType(declaration: TSNode) =
        when (declaration.type) {
            "class_declaration", "record_declaration" -> NodeType.CLASS
            "interface_declaration" -> NodeType.INTERFACE
            "enum_declaration" -> NodeType.ENUM
            "annotation_type_declaration" -> NodeType.ANNOTATION
            else -> NodeType.UNKNOWN
        }
}
