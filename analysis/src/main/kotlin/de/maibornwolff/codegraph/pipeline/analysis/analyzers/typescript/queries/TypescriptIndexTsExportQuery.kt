package de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.queries

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.model.toImport
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.nodeAsString
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.resolveRelativePath
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.model.IdentifierWithAlias
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.trimFileEnding
import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency
import de.maibornwolff.codegraph.pipeline.analysis.model.Path
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TreeSitterTypescript

/**
 *  [execute]
 */
class TypescriptIndexTsExportQuery(
    val typescript: TreeSitterTypescript
) {
    private val query = TSQuery(typescript, "(export_statement (export_clause) @export source: _ @source)")

    /**
     * Returns the dependencies and aliases of the exports contained within the given node.
     * Relative paths are resolved based on the current file path.
     * For each export, an implicit index file is added to the dependencies.
     *
     * Example:
     * ```
     * //file: ./src/index.ts
     * export { foo as bar } from './foo';
     * export { baz } from './baz';
     * ```
     * will result in the following identifiers (with aliases) and dependencies:
     * ```
     * IdentifierWithAlias(identifier = "foo", alias = "bar") to setOf(
     *     Dependency(path = Path("src/foo/foo")),
     *     Dependency(path = Path("src/foo/index/foo"))
     * ),
     * IdentifierWithAlias(identifier = "baz", alias = null) to setOf(
     *     Dependency(path = Path("src/baz/baz")),
     *     Dependency(path = Path("src/baz/index/baz"))
     * )
     * ```
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @param currentFilePath the path of the current file
     * @return [IdentifierWithAlias] and all [Dependency]s for each export
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String,
        currentFilePath: Path
    ): List<Pair<IdentifierWithAlias, Set<Dependency>>> =
        node
            .execute(query)
            .flatMap {
                val exports = it.captures[0].node.getNamedChildren().map { child ->
                    identifierWithAlias(child, bodyContainingNode)
                }
                val source = it.captures[1].node.getNamedChild(0)
                val trimmedSourceName = nodeAsString(source, bodyContainingNode).trimFileEnding()
                val importPath = resolveRelativePath(trimmedSourceName.toImport(), currentFilePath)
                exports.map { export -> export to dependencyAndImplicitIndexDependency(importPath, export.identifier) }
            }

    private fun identifierWithAlias(
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
