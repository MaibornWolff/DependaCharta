package de.maibornwolff.dependacharta.pipeline.analysis.analyzers

import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.treesitter.excavationsite.api.DeclarationType
import de.maibornwolff.treesitter.excavationsite.api.ImportDeclaration
import de.maibornwolff.treesitter.excavationsite.api.UsedType

// The sentinel name TSE assigns to default export declarations (e.g. `export default ...`)
const val TSE_DEFAULT_EXPORT_NAME = "DEFAULT_EXPORT"

// The sentinel name TSE assigns to wildcard re-export declarations (e.g. `export * from '...'`)
const val WILDCARD_EXPORT_NAME = "*"

fun ImportDeclaration.toDependency(): Dependency {
    return Dependency(path = Path(path), isWildcard = isWildcard)
}

fun UsedType.toType(): Type {
    if (genericTypes.isEmpty()) {
        return Type.simple(name)
    }
    return Type.generic(name, genericTypes.map { it.toType() })
}

fun DeclarationType.toNodeType(): NodeType {
    return when (this) {
        DeclarationType.CLASS, DeclarationType.RECORD -> NodeType.CLASS
        DeclarationType.INTERFACE -> NodeType.INTERFACE
        DeclarationType.ENUM -> NodeType.ENUM
        DeclarationType.ANNOTATION -> NodeType.ANNOTATION
        DeclarationType.FUNCTION -> NodeType.FUNCTION
        DeclarationType.VARIABLE -> NodeType.VARIABLE
        DeclarationType.REEXPORT -> NodeType.REEXPORT
        DeclarationType.UNKNOWN -> NodeType.UNKNOWN
    }
}
