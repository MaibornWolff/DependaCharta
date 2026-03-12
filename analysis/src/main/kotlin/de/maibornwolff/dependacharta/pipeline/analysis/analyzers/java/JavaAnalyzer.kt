package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.java

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.model.*
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.treesitter.excavationsite.api.Language
import de.maibornwolff.treesitter.excavationsite.api.TreeSitterDependencies
import de.maibornwolff.treesitter.excavationsite.shared.domain.Declaration
import de.maibornwolff.treesitter.excavationsite.shared.domain.DeclarationType
import de.maibornwolff.treesitter.excavationsite.shared.domain.DependencyResult
import de.maibornwolff.treesitter.excavationsite.shared.domain.ImportDeclaration
import de.maibornwolff.treesitter.excavationsite.shared.domain.UsedType

class JavaAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    override fun analyze(): FileReport {
        val result = TreeSitterDependencies.analyze(fileInfo.content, Language.JAVA)
        val nodes = result.declarations.map { declaration ->
            toNode(result, declaration)
        }
        return FileReport(nodes)
    }

    private fun toNode(
        result: DependencyResult,
        declaration: Declaration,
    ): Node {
        val implicitWildcardImport = Dependency(path = Path(result.packagePath), isWildcard = true)
        val imports = result.imports.map { toImportDependency(it) }
        return Node(
            pathWithName = Path(result.packagePath + declaration.name),
            physicalPath = fileInfo.physicalPath,
            language = SupportedLanguage.JAVA,
            nodeType = toNodeType(declaration.type),
            dependencies = (imports + implicitWildcardImport).toSet(),
            usedTypes = declaration.usedTypes.map { toType(it) }.toSet()
        )
    }

    private fun toImportDependency(import_: ImportDeclaration): Dependency {
        return Dependency(path = Path(import_.path), isWildcard = import_.isWildcard)
    }

    private fun toType(usedType: UsedType): Type {
        if (usedType.genericTypes.isEmpty()) {
            return Type.simple(usedType.name)
        }
        return Type.generic(usedType.name, usedType.genericTypes.map { toType(it) })
    }

    private fun toNodeType(type: DeclarationType): NodeType {
        return when (type) {
            DeclarationType.CLASS, DeclarationType.RECORD -> NodeType.CLASS
            DeclarationType.INTERFACE -> NodeType.INTERFACE
            DeclarationType.ENUM -> NodeType.ENUM
            DeclarationType.ANNOTATION -> NodeType.ANNOTATION
            DeclarationType.UNKNOWN -> NodeType.UNKNOWN
        }
    }
}
