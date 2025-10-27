package de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.queries

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.execute
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.getNamedChildren
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.extractType
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp.CSharpQueryFactory
import org.treesitter.TSNode

/**
 *  [execute]
 */
class CSharpInheritanceTypesQuery {
    private val query = CSharpQueryFactory.getQuery("(base_list) @inheritance")

    /**
     * Returns the types of classes or interfaces a class extends or implements.
     *
     * Example:
     * ```
     * public class MyGreatClass: MyGreatBaseClass, MyGreatInterface {}
     *
     * ```
     * will return Types containing `MyGreatBaseClass` and `MyGreatInterface`
     *
     * @param node the node to execute the query on
     * @param bodyContainingNode the string that was parsed to get to the node
     * @return list of [de.maibornwolff.codegraph.pipeline.analysis.model.Type] objects representing the types of the
     * interfaces that are implemented or classes that are extended
     */
    fun execute(
        node: TSNode,
        bodyContainingNode: String
    ) = node
        .execute(query)
        .flatMap {
            it.captures[0]
                .node
                .getNamedChildren()
                .map { type -> extractType(type, bodyContainingNode) }
        }
}
