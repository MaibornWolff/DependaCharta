package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.python

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.python.queries.PythonDefinitionsQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.python.queries.PythonImportQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.python.queries.PythonTypeAttributeQuery
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.python.queries.PythonTypeIdentifierQuery
import de.maibornwolff.dependacharta.pipeline.analysis.model.*
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterPython

class PythonAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    private val python = TreeSitterPython()
    private val definitionsQuery = PythonDefinitionsQuery(python)
    private val importQuery = PythonImportQuery(python)
    private val identifierQuery = PythonTypeIdentifierQuery(python)
    private val attributeQuery = PythonTypeAttributeQuery(python)

    override fun analyze(): FileReport {
        val rootNode = parseCode(fileInfo.content)
        val filePathWithSlashes = fileInfo.physicalPathAsPath()
        val pathWithoutFileEnding = filePathWithSlashes.withoutFileSuffix("py")
        val modulePath = pathWithoutFileEnding.parts

        val definitions = definitionsQuery.execute(rootNode)

        val importsFrom = importQuery.executeImportsFrom(rootNode, fileInfo.content, modulePath)
        val wildcardImportsFrom = importQuery.executeWildcardImportsFrom(rootNode, fileInfo.content, modulePath)
        val aliasedImportsFrom = importQuery.executeAliasedImportsFrom(rootNode, fileInfo.content, modulePath)

        val imports = importQuery.executeImports(rootNode, fileInfo.content)
        val aliasedImports = importQuery.executeAliasedImports(rootNode, fileInfo.content, modulePath)

        val nodes = mutableListOf<Node>()

        if (modulePath.last() == "__init__") {
            nodes.addAll(
                importsFrom.filter { dependency -> "__init__" !in dependency.path.parts }.map { dependency ->
                    Node(
                        pathWithName = Path(modulePath + dependency.path.parts.last()),
                        physicalPath = fileInfo.physicalPath,
                        language = SupportedLanguage.PYTHON,
                        nodeType = NodeType.UNKNOWN,
                        dependencies = importsFrom.toSet(),
                        usedTypes = setOf(Type.simple(dependency.path.parts.last()))
                    )
                }
            )
        }

        if (definitions.isNotEmpty()) {
            nodes.addAll(
                definitions.map {
                    extractNodeFromDefinition(
                        modulePath,
                        it,
                        importsFrom + wildcardImportsFrom,
                        aliasedImportsFrom,
                        imports,
                        aliasedImports,
                    )
                }
            )
        }

        return FileReport(nodes)
    }

    private fun parseCode(pythonCode: String): TSNode {
        val parser = TSParser()
        parser.language = python
        val tree = parser.parseString(null, pythonCode)
        return tree.rootNode
    }

    private fun extractNodeFromDefinition(
        modulePath: List<String>,
        definition: TSNode,
        importFromDependencies: List<Dependency>,
        aliasedImportsFrom: Map<String, String>,
        imports: List<String>,
        aliasedImports: Map<String, String>,
    ): Node {
        val mutableImportFromDependencies = importFromDependencies.toMutableList()
        val nodeBody = nodeAsString(definition, fileInfo.content)
        val definitionNode = parseCode(nodeBody).getChild(0)

        if (definition.type == "identifier") {
            return Node(
                pathWithName = Path(modulePath + nodeBody),
                physicalPath = fileInfo.physicalPath,
                language = SupportedLanguage.PYTHON,
                nodeType = NodeType.VARIABLE,
                dependencies = setOf(),
                usedTypes = setOf()
            )
        }

        var classOrFunctionDefinitionNode = definitionNode
        if (definition.type == "decorated_definition") {
            classOrFunctionDefinitionNode = definitionNode.getChildByFieldName("definition")
        }

        val definitionName = nodeAsString(classOrFunctionDefinitionNode.getChildByFieldName("name"), nodeBody)

        val importFromTypes = buildImportFromTypes(
            identifierQuery.execute(definitionNode, nodeBody),
            aliasedImportsFrom,
            mutableImportFromDependencies
        )

        val importDependencies = buildImportDependencies(imports, aliasedImports, definitionNode, nodeBody)

        return Node(
            pathWithName = Path(modulePath + definitionName),
            physicalPath = fileInfo.physicalPath,
            language = SupportedLanguage.PYTHON,
            nodeType = nodeType(classOrFunctionDefinitionNode),
            dependencies = mutableImportFromDependencies.toSet() + importDependencies,
            usedTypes = importFromTypes
        )
    }

    private fun nodeType(declaration: TSNode) =
        when (declaration.type) {
            "class_definition" -> NodeType.CLASS
            "function_definition" -> NodeType.FUNCTION
            else -> NodeType.UNKNOWN
        }

    private fun buildImportFromTypes(
        types: List<String>,
        aliasedImportsFrom: Map<String, String>,
        mutableDependencies: MutableList<Dependency>
    ): Set<Type> =
        types
            .map {
                val aliasedType = checkAliasImportFrom(it, aliasedImportsFrom, mutableDependencies)
                Type.simple(aliasedType)
            }.toSet()

    private fun checkAliasImportFrom(
        type: String,
        aliasedImportsFrom: Map<String, String>,
        mutableDependencies: MutableList<Dependency>
    ): String {
        if (type in aliasedImportsFrom) {
            val aliasedImport = aliasedImportsFrom[type]!!.split(".")
            val aliasedType = aliasedImport.last()
            mutableDependencies.add(Dependency(Path(aliasedImport)))
            mutableDependencies.add(Dependency(Path(aliasedImport.dropLast(1) + listOf("__init__", aliasedType))))
            return aliasedType
        }

        return type
    }

    private fun buildImportDependencies(
        imports: List<String>,
        aliasedImports: Map<String, String>,
        definitionNode: TSNode,
        nodeBody: String
    ): Set<Dependency> {
        val dependencies = mutableSetOf<Dependency>()
        if (imports.isNotEmpty() || aliasedImports.isNotEmpty()) {
            val attributes = attributeQuery.execute(definitionNode, nodeBody)
            attributes.forEach { attribute ->
                val attributeList = attribute.split(".").toMutableList()
                if (attributeList.size >= 2) {
                    val attributeType = attributeList.removeLast()
                    val attributeIdentifier = attributeList.joinToString(".")
                    if (attributeIdentifier in imports) {
                        dependencies.add(Dependency(Path(attributeList + listOf(attributeType)), false))
                        dependencies.add(Dependency(Path(attributeList + listOf("__init__", attributeType)), false))
                    } else if (attributeIdentifier in aliasedImports) {
                        val aliasedPath = Path(aliasedImports[attributeIdentifier]!!.split(".") + listOf(attributeType))
                        dependencies.add(Dependency(aliasedPath, false))
                        val aliasedInitPath =
                            Path(aliasedImports[attributeIdentifier]!!.split(".") + listOf("__init__", attributeType))
                        dependencies.add(Dependency(aliasedInitPath, false))
                    }
                }
            }
        }
        return dependencies
    }
}
