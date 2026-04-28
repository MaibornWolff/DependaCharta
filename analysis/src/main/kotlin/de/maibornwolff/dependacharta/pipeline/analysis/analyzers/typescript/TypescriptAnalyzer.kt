package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.typescript

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.BaseLanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.stripSourceFileExtension
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.toRelativePath
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils.withoutFileSuffix
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
import de.maibornwolff.treesitter.excavationsite.api.Language
import de.maibornwolff.treesitter.excavationsite.api.TreeSitterDependencies
import java.io.File

// "DEFAULT_EXPORT" is the name TSE assigns to default export declarations
private const val DEFAULT_EXPORT_NODE_NAME = "DEFAULT_EXPORT"

class TypescriptAnalyzer(
    fileInfo: FileInfo,
) : BaseLanguageAnalyzer(fileInfo) {
    override val language = SupportedLanguage.TYPESCRIPT

    override fun tseLanguage(): Language = if (fileInfo.physicalPath.endsWith(".tsx")) Language.TSX else Language.TYPESCRIPT

    override fun buildPathWithName(
        packagePath: List<String>,
        declaration: Declaration,
    ): Path {
        if (declaration.parentPath.isNotEmpty()) {
            return Path(declaration.parentPath) + declaration.name
        }
        val extension = if (fileInfo.physicalPath.endsWith(".tsx")) "tsx" else "ts"
        val filePath = fileInfo.physicalPathAsPath().withoutFileSuffix(extension)
        if (declaration.name == DEFAULT_EXPORT_NODE_NAME) {
            val qualifiedName = filePath.parts.joinToString("_") + "_$DEFAULT_EXPORT_NODE_NAME"
            return filePath + qualifiedName
        }
        return filePath + declaration.name
    }

    override fun analyze(): FileReport {
        val baseReport = super.analyze()
        val analysisRoot = fileInfo.analysisRoot ?: return baseReport
        val extension = if (fileInfo.physicalPath.endsWith(".tsx")) "tsx" else "ts"
        val currentFilePath = fileInfo.physicalPathAsPath().withoutFileSuffix(extension)
        val nodes = baseReport.nodes.flatMap { node ->
            if (node.pathWithName.parts.lastOrNull() == "*") {
                expandWildcardReexport(node, currentFilePath, analysisRoot)
            } else {
                listOf(node)
            }
        }
        return FileReport(nodes)
    }

    override fun selectImports(
        declaration: Declaration,
        imports: List<ImportDeclaration>
    ): List<ImportDeclaration> {
        if (declaration.type != DeclarationType.REEXPORT) return imports
        if (declaration.name == "*") return imports
        val reexportNames = setOf(declaration.name) + declaration.usedTypes.map { it.name }
        return imports.filter { it.path.isNotEmpty() && it.path.last() in reexportNames }
    }

    override fun extraDependencies(declaration: Declaration): Set<Dependency> {
        if (declaration.name != DEFAULT_EXPORT_NODE_NAME) return emptySet()
        val extension = if (fileInfo.physicalPath.endsWith(".tsx")) "tsx" else "ts"
        val filePath = fileInfo.physicalPathAsPath().withoutFileSuffix(extension)
        return setOf(Dependency(path = filePath, isWildcard = true))
    }

    override fun convertImport(import: ImportDeclaration): Set<Dependency> {
        val resolvedPath = resolveImportPath(import.path)
        val primary = Dependency(path = Path(resolvedPath), isWildcard = import.isWildcard)
        val index = buildIndexDependency(resolvedPath, import.isWildcard)
        return if (index != null) setOf(primary, index) else setOf(primary)
    }

    // Adds import specifier names as usedTypes so the pipeline can resolve function call usages
    // (e.g. `foo()` where `foo` was imported). For REEXPORT nodes this overlaps with
    // declaration.usedTypes from TSE, which is harmless.
    override fun extraUsedTypes(imports: List<ImportDeclaration>): Set<Type> {
        return imports
            .filter { !it.isWildcard && it.path.isNotEmpty() }
            .mapNotNull { import ->
                val specifier = import.path.last().stripSourceFileExtension()
                if (specifier.isEmpty() || specifier == DEFAULT_EXPORT_NODE_NAME) null else Type.simple(specifier)
            }.toSet()
    }

    // Nodes here are synthetic — constructed from a secondary TSE parse of the source file,
    // not mapped from the current file's declarations. TseMappings extension functions are
    // not used because there is no ImportDeclaration/UsedType/DeclarationType to map from.
    private fun expandWildcardReexport(
        wildcardNode: Node,
        currentFilePath: Path,
        analysisRoot: File
    ): List<Node> {
        val wildcardDeps = wildcardNode.dependencies.filter { it.isWildcard }
        val sourceFiles = wildcardDeps.mapNotNull { resolveSourceFile(it.path.parts, analysisRoot) }.toSet()
        if (sourceFiles.isEmpty()) return listOf(wildcardNode)
        return sourceFiles.flatMap { sourceFile ->
            val sourceRelPath = toRelativePath(sourceFile, analysisRoot, stripExtension = true).parts
            val srcLanguage = if (sourceFile.name.endsWith(".tsx")) Language.TSX else Language.TYPESCRIPT
            val sourceTseResult = TreeSitterDependencies.analyze(sourceFile.readText(), srcLanguage)
            sourceTseResult.declarations.map { decl ->
                Node(
                    pathWithName = currentFilePath + decl.name,
                    physicalPath = wildcardNode.physicalPath,
                    language = language,
                    nodeType = NodeType.REEXPORT,
                    dependencies = setOf(Dependency(path = Path(sourceRelPath + decl.name))),
                    usedTypes = setOf(Type.simple(decl.name))
                )
            }
        }
    }

    private fun resolveSourceFile(
        pathParts: List<String>,
        analysisRoot: File
    ): File? {
        val pathStr = pathParts.joinToString("/")
        for (candidate in listOf("$pathStr.ts", "$pathStr.tsx", "$pathStr/index.ts", "$pathStr/index.tsx")) {
            val file = File(analysisRoot, candidate)
            if (file.exists()) return file
        }
        return null
    }

    private fun buildIndexDependency(
        resolvedPath: List<String>,
        isWildcard: Boolean
    ): Dependency? {
        if (isWildcard) {
            return if (resolvedPath.isEmpty()) {
                null
            } else {
                Dependency(Path(resolvedPath + "index"), isWildcard = true)
            }
        }
        return if (resolvedPath.size < 2) {
            null
        } else {
            Dependency(Path(resolvedPath.dropLast(1) + listOf("index") + resolvedPath.takeLast(1)))
        }
    }
}
