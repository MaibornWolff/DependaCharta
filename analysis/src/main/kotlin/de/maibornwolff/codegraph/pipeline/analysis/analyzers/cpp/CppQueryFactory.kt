package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp

import org.treesitter.TSQuery
import org.treesitter.TreeSitterCpp

class CppQueryFactory {
    companion object {
        val cppLanguage: TreeSitterCpp = TreeSitterCpp()
        val queries: MutableMap<String, TSQuery> = mutableMapOf()

        fun getQuery(query: String): TSQuery {
            val trimmedQuery = query.trimIndent()
            return queries.getOrPut(trimmedQuery, { TSQuery(cppLanguage, trimmedQuery) })
        }
    }
}