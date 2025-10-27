package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp

import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency
import de.maibornwolff.codegraph.pipeline.analysis.model.Path
import de.maibornwolff.codegraph.pipeline.analysis.model.Type

internal class FunctionArgumentParser {
    companion object {
        fun intoTypes(argumentsString: String): List<Type> {
            val arguments = splitOnCommasNotInDiamonds(argumentsString)

            return if (arguments.size > 1) {
                arguments.flatMap(::intoTypes)
            } else {
                val innerTypes = getInnerTypes(argumentsString)
                extractNames(arguments).map { Type.generic(it, innerTypes) }
            }
        }

        fun intoDependencies(argumentsString: String): Set<Dependency> {
            val arguments = argumentsString.replace(">", "").split('<', ',')

            return if (arguments.size > 1) {
                arguments
                    .flatMap(::intoDependencies)
                    .toSet()
            } else {
                extractNamespaces(arguments)
                    .map { Dependency(it, isWildcard = true) }
                    .toSet()
            }
        }

        private fun extractNames(fullyQualifiedNames: List<String>) =
            extractPotentialNames(fullyQualifiedNames)
                .map { it.substringAfterLast("::").trim() }

        private fun extractNamespaces(fullyQualifiedNames: List<String>) =
            extractPotentialNames(fullyQualifiedNames)
                .filter { it.contains("::") }
                .map { it.substringBeforeLast("::").trim() }
                .map { Path(it.split("::")) }

        private fun splitOnCommasNotInDiamonds(input: String): List<String> {
            val result = mutableListOf<String>()
            val current = StringBuilder()
            var depth = 0

            for (c in input) {
                when (c) {
                    '<' -> {
                        depth++
                        current.append(c)
                    }

                    '>' -> {
                        depth--
                        current.append(c)
                    }

                    ',' -> {
                        if (depth == 0) {
                            result.add(current.toString().trim())
                            current.clear()
                        } else {
                            current.append(c)
                        }
                    }

                    else -> current.append(c)
                }
            }
            if (current.isNotEmpty()) {
                result.add(current.toString().trim())
            }
            return result
        }

        private fun extractPotentialNames(potentialNames: List<String>): List<String> =
            potentialNames.map {
                it.substringBefore('<').trim()
            }

        private fun getInnerTypes(arguments: String): List<Type> =
            if (arguments.contains('<')) {
                val begin = arguments.indexOf('<') + 1
                val end = findIndexOfClosingDiamond(arguments) - 1
                if (begin < end) {
                    intoTypes(arguments.substring(begin, end))
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
    }
}
