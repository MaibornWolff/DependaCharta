package de.maibornwolff.codegraph.pipeline.analysis.analyzers.csharp

import org.treesitter.TSQuery
import org.treesitter.TreeSitterCSharp

class CSharpQueryFactory {
    companion object {
        val CsharpLanguage: TreeSitterCSharp = TreeSitterCSharp()
        val queries: MutableMap<String, TSQuery> = mutableMapOf()

        fun getQuery(query: String): TSQuery {
            val trimmedQuery = query.trimIndent()
            return queries.getOrPut(trimmedQuery, { TSQuery(CsharpLanguage, trimmedQuery) })
        }
    }
}