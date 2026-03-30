package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.kotlin

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.model.*
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import de.maibornwolff.treesitter.excavationsite.api.Declaration
import de.maibornwolff.treesitter.excavationsite.api.DeclarationType
import de.maibornwolff.treesitter.excavationsite.api.ImportDeclaration
import de.maibornwolff.treesitter.excavationsite.api.Language
import de.maibornwolff.treesitter.excavationsite.api.TreeSitterDependencies
import de.maibornwolff.treesitter.excavationsite.api.UsedType

class KotlinAnalyzer(
    private val fileInfo: FileInfo
) : LanguageAnalyzer {
    override fun analyze(): FileReport {
        val result = TreeSitterDependencies.analyze(fileInfo.content, Language.KOTLIN)
        val implicitWildcardImport = Dependency(path = Path(result.packagePath), isWildcard = true)
        val dependencies = (result.imports.map { toDependency(it) } + implicitWildcardImport).toSet()
        val nodes = result.declarations.map { declaration ->
            toNode(result.packagePath, dependencies, declaration)
        }
        return FileReport(nodes)
    }

    private fun toNode(
        packagePath: List<String>,
        dependencies: Set<Dependency>,
        declaration: Declaration,
    ): Node {
        return Node(
            pathWithName = Path(packagePath + declaration.parentPath + declaration.name),
            physicalPath = fileInfo.physicalPath,
            language = SupportedLanguage.KOTLIN,
            nodeType = toNodeType(declaration.type),
            dependencies = dependencies,
            usedTypes = declaration.usedTypes.map { toType(it) }.toSet()
        )
    }

    private fun toDependency(importDeclaration: ImportDeclaration): Dependency {
        return Dependency(path = Path(importDeclaration.path), isWildcard = importDeclaration.isWildcard)
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
