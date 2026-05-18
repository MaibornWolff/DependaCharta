package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.python

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.toNodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.treesitter.excavationsite.api.Declaration
import de.maibornwolff.treesitter.excavationsite.api.DeclarationType
import de.maibornwolff.treesitter.excavationsite.api.ImportDeclaration
import de.maibornwolff.treesitter.excavationsite.api.ImportKind
import de.maibornwolff.treesitter.excavationsite.api.Language
import de.maibornwolff.treesitter.excavationsite.api.TreeSitterDependencies
import de.maibornwolff.treesitter.excavationsite.api.UsedType

class PythonAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    private companion object {
        const val INIT = "__init__"
        const val PY_SUFFIX = "py"
        const val SEPARATOR = "."
    }

    override fun analyze(): FileReport {
        val result = TreeSitterDependencies.analyze(fileInfo.content, Language.PYTHON)
        val modulePath = fileInfo.physicalPathAsPath().withoutFileSuffix(PY_SUFFIX).parts

        val resolvedImports = result.imports.map { it.resolveRelative(modulePath) }
        val regularFromImports = resolvedImports.filter { it.kind == ImportKind.IMPORT_FROM && !it.isAliased && !it.isWildcard }
        val wildcardFromImports = resolvedImports.filter { it.kind == ImportKind.IMPORT_FROM && it.isWildcard }
        val aliasedFromImports = resolvedImports.filter { it.kind == ImportKind.IMPORT_FROM && it.isAliased }
        val standardImports = resolvedImports.filter { it.kind == ImportKind.STANDARD }

        val regularFromTwins = regularFromImports.flatMap { it.twinPair() }
        val wildcardFromTwins = wildcardFromImports.flatMap { it.twinPair() }
        val basePreloadedDeps = regularFromTwins + wildcardFromTwins

        val nodes = mutableListOf<Node>()
        if (modulePath.lastOrNull() == INIT) {
            nodes.addAll(initFileReexportNodes(regularFromImports, regularFromTwins.toSet(), modulePath))
        }
        result.declarations.forEach { declaration ->
            nodes.add(buildDeclarationNode(declaration, modulePath, basePreloadedDeps, aliasedFromImports, standardImports))
        }
        return FileReport(nodes)
    }

    private fun initFileReexportNodes(
        regularFromImports: List<ImportDeclaration>,
        regularFromTwins: Set<Dependency>,
        modulePath: List<String>
    ): List<Node> =
        regularFromImports.map { import ->
            val name = import.path.last()
            Node(
                pathWithName = Path(modulePath + name),
                physicalPath = fileInfo.physicalPath,
                language = SupportedLanguage.PYTHON,
                nodeType = NodeType.UNKNOWN,
                dependencies = regularFromTwins,
                usedTypes = setOf(Type.simple(name))
            )
        }

    private fun buildDeclarationNode(
        declaration: Declaration,
        modulePath: List<String>,
        basePreloadedDeps: List<Dependency>,
        aliasedFromImports: List<ImportDeclaration>,
        standardImports: List<ImportDeclaration>
    ): Node {
        val pathWithName = Path(modulePath + declaration.name)
        val nodeType = declaration.type.toNodeType()

        if (declaration.type == DeclarationType.VARIABLE) {
            return Node(
                pathWithName = pathWithName,
                physicalPath = fileInfo.physicalPath,
                language = SupportedLanguage.PYTHON,
                nodeType = nodeType,
                dependencies = emptySet(),
                usedTypes = emptySet()
            )
        }

        val (identifierStream, attributeStream) = declaration.usedTypes.partition { it.namespacePrefix.isEmpty() }
        val identifierNames = identifierStream.map { it.name }.toSet()

        val deps = basePreloadedDeps.toMutableSet()
        aliasedFromImports.forEach { import ->
            if (import.path.last() in identifierNames) {
                deps.addAll(import.twinPair())
            }
        }
        attributeStream.forEach { ut ->
            deps.addAll(matchStandardImportAttribute(ut, standardImports))
        }

        val usedTypes = identifierStream.map { Type.simple(it.name) }.toSet()
        return Node(
            pathWithName = pathWithName,
            physicalPath = fileInfo.physicalPath,
            language = SupportedLanguage.PYTHON,
            nodeType = nodeType,
            dependencies = deps,
            usedTypes = usedTypes
        )
    }

    private fun matchStandardImportAttribute(
        usedType: UsedType,
        standardImports: List<ImportDeclaration>
    ): List<Dependency> {
        val joinedPrefix = usedType.namespacePrefix.joinToString(SEPARATOR)
        return standardImports.flatMap { import ->
            if (import.path.joinToString(SEPARATOR) == joinedPrefix) {
                listOf(
                    Dependency(Path(import.path + usedType.name)),
                    Dependency(Path(import.path + INIT + usedType.name))
                )
            } else {
                emptyList()
            }
        }
    }

    private fun ImportDeclaration.resolveRelative(modulePath: List<String>): ImportDeclaration {
        val first = path.firstOrNull() ?: return this
        if (first.isEmpty() || !first.all { it == '.' }) return this
        val resolvedPath = modulePath.dropLast(first.length) + path.drop(1)
        return copy(path = resolvedPath)
    }

    private fun ImportDeclaration.twinPair(): List<Dependency> {
        val canonical = Dependency(Path(path), isWildcard)
        val initPath = if (isWildcard) {
            path + INIT
        } else {
            path.dropLast(1) + INIT + path.last()
        }
        return listOf(canonical, Dependency(Path(initPath), isWildcard))
    }
}
