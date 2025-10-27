package de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.queries

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.model.toImport
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.resolveRelativePath
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.DEFAULT_EXPORT_NODE_NAME
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.model.DependenciesAndAliases
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.model.IdentifierWithAlias
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.trimFileEnding
import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency
import de.maibornwolff.codegraph.pipeline.analysis.model.Path
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TSQueryMatch
import org.treesitter.TreeSitterTypescript

/**
 *  [execute]
 */
class TypescriptImportStatementQuery(
    val typescript: TreeSitterTypescript
) {
    private val esModuleImportQuery = TSQuery(typescript, "(import_statement (import_clause) @import source: _ @source)")
    private val commonJsImportsQuery = TSQuery(
        typescript,
        "(variable_declarator name: _ @name value: (call_expression function: (identifier) @function arguments: (arguments (string) @module) (#eq? @function \"require\")))"
    )

    /**
     * Returns the dependencies and aliases of the imports contained within the given node.
     * Relative paths are resolved based on the current file path.
     * For each import, an implicit index file is added to the dependencies (should the export be part of an index.ts file).
     * This supports both ES Module syntax (`import { foo as bar } from 'foo'`) and CommonJS syntax (`const { foo: bar } = require('foo')`).
     *
     * Example:
     * ```
     * //file: ./src/index.ts
     * import esModuleDefaultExport from './esModule/defaultExport';
     * import { esModuleFoo as esModuleBar } from './esModule/foo';
     *
     * const commonJsDefaultExport = require('./commonJs/defaultExport');
     * const { commonJsFoo: commonJsBar } = require('./commonJs/foo');
     * ```
     * will result in the following dependencies and aliases:
     * ```
     * DependenciesAndAliases(
     *    dependencies = setOf(
     *      Dependency(path = Path("src/esModule/defaultExport/src_esModule_defaultExport_DEFAULT_EXPORT")),
     *      Dependency(path = Path("src/esModule/defaultExport/index/src_esModule_defaultExport_DEFAULT_EXPORT")),
     *      Dependency(path = Path("src/esModule/foo/esModuleFoo")),
     *      Dependency(path = Path("src/esModule/foo/index/esModuleFoo")),
     *      Dependency(path = Path("src/commonJs/defaultExport/src_commonJs_defaultExport_DEFAULT_EXPORT")),
     *      Dependency(path = Path("src/commonJs/defaultExport/index/src_commonJs_defaultExport_DEFAULT_EXPORT")),
     *      Dependency(path = Path("src/commonJs/foo/commonJsFoo")),
     *      Dependency(path = Path("src/commonJs/foo/index/commonJsFoo"))
     *    ),
     *    importByAlias = mapOf(
     *      "esModuleDefaultExport" to "src_esModule_defaultExport_DEFAULT_EXPORT",
     *      "exModuleBar" to "esModuleFoo",
     *      "commonJsDefaultExport" to "src_commonJs_defaultExport_DEFAULT_EXPORT",
     *      "commonJsBar" to "commonJsFoo"
     *    )
     * )
     * ```
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @param currentFilePath the path of the file that contains the node
     * @return [DependenciesAndAliases] containing the imported dependencies and aliases
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String,
        currentFilePath: Path
    ): DependenciesAndAliases {
        val esModuleImports = node
            .execute(esModuleImportQuery)
            .map { import -> extractDependenciesAndAliasesFromEsModule(import, bodyContainingNode, currentFilePath) }
        val commonJsImports = node
            .execute(commonJsImportsQuery)
            .map { import -> extractDependenciesAndAliasesFromCommonJs(import, bodyContainingNode, currentFilePath) }

        return (esModuleImports + commonJsImports).fold(
            DependenciesAndAliases(emptySet(), emptyMap()),
            DependenciesAndAliases::plus
        )
    }

    private fun extractDependenciesAndAliasesFromEsModule(
        import: TSQueryMatch,
        nodeBody: String,
        filePath: Path
    ): DependenciesAndAliases {
        val source = import.captures[1].node.getNamedChild(0)
        val sourceName = nodeAsString(source, nodeBody)
        val trimmedSourceName = sourceName.trimFileEnding()
        val path = resolveRelativePath(trimmedSourceName.toImport(), filePath)

        val identifiersWithAliases = import.captures[0]
            .node
            .getNamedChildren()
            .flatMap { child ->
                if (child.type == "named_imports") {
                    child.getNamedChildren().map { grandchild -> identifierWithAliasEsModule(grandchild, nodeBody) }
                } else {
                    listOf(
                        IdentifierWithAlias(
                            identifier = (path + DEFAULT_EXPORT_NODE_NAME).withUnderscores(),
                            alias = nodeAsString(child, nodeBody)
                        )
                    )
                }
            }
        return toDependenciesAndAliases(identifiersWithAliases, path)
    }

    private fun extractDependenciesAndAliasesFromCommonJs(
        import: TSQueryMatch,
        nodeBody: String,
        filePath: Path
    ): DependenciesAndAliases {
        val source = import.captures[2].node.getNamedChild(0)
        val sourceName = nodeAsString(source, nodeBody)
        val trimmedSourceName = sourceName.trimFileEnding()
        val path = resolveRelativePath(trimmedSourceName.toImport(), filePath)
        val node = import.captures[0].node
        val identifiersWithAliases = if (node.type == "object_pattern") {
            node.getNamedChildren().map { child -> identifierWithAliasCommonJs(child, nodeBody) }
        } else {
            listOf(
                IdentifierWithAlias(identifier = (path + DEFAULT_EXPORT_NODE_NAME).withUnderscores(), alias = nodeAsString(node, nodeBody))
            )
        }
        return toDependenciesAndAliases(identifiersWithAliases, path)
    }

    private fun toDependenciesAndAliases(
        identifiersWithAliases: List<IdentifierWithAlias>,
        path: Path,
    ): DependenciesAndAliases {
        val dependencies = identifiersWithAliases
            .flatMap { identifierWithAlias ->
                dependencyAndImplicitIndexDependency(path, identifierWithAlias.identifier)
            }.toSet()

        val identifiersByAlias = identifiersWithAliases
            .mapNotNull { if (it.alias != null) it.alias to it.identifier else null }
            .toMap()

        return DependenciesAndAliases(
            dependencies = dependencies,
            importByAlias = identifiersByAlias
        )
    }

    private fun identifierWithAliasCommonJs(
        child: TSNode,
        nodeBody: String
    ): IdentifierWithAlias {
        if (child.type == "pair_pattern") {
            val identifier = nodeAsString(child.getChildByFieldName("key"), nodeBody)
            val alias = nodeAsString(child.getChildByFieldName("value"), nodeBody)
            return IdentifierWithAlias(identifier = identifier, alias = alias)
        }
        return IdentifierWithAlias(identifier = nodeAsString(child, nodeBody), alias = null)
    }

    private fun identifierWithAliasEsModule(
        child: TSNode,
        nodeBody: String
    ): IdentifierWithAlias {
        val identifier = nodeAsString(child.getChildByFieldName("name"), nodeBody)
        val aliasNode = child.getChildByFieldName("alias")
        val alias = if (aliasNode.isNull) null else nodeAsString(aliasNode, nodeBody)
        return IdentifierWithAlias(identifier = identifier, alias = alias)
    }

    private fun dependencyAndImplicitIndexDependency(
        importPath: Path,
        export: String
    ) = setOf(
        Dependency(importPath + export),
        Dependency(importPath + "index" + export)
    )
}
