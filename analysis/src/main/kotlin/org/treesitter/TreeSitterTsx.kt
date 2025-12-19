package org.treesitter

import org.treesitter.utils.NativeUtils

/**
 * TSX language grammar for Tree-sitter.
 *
 * This class provides access to the TSX grammar which properly handles JSX syntax
 * embedded in TypeScript files. TSX is essential for correctly parsing React components
 * and other JSX-based code without generating parse errors.
 *
 * ## Usage
 * ```kotlin
 * val parser = TSParser()
 * parser.language = TreeSitterTsx()
 * val tree = parser.parseString(null, tsxCode)
 * ```
 *
 * ## Difference from TreeSitterTypescript
 * - **TreeSitterTypescript**: For .ts files, no JSX support
 * - **TreeSitterTsx**: For .tsx files, full JSX support
 *
 * Using the wrong grammar will result in parse errors:
 * - Parsing .tsx with TypeScript grammar → hasError: true (JSX treated as errors)
 * - Parsing .tsx with TSX grammar → hasError: false (JSX properly recognized)
 *
 * @see TreeSitterTypescript for TypeScript-only grammar
 */
class TreeSitterTsx : TSLanguage {
    companion object {
        init {
            NativeUtils.loadLib("lib/tree-sitter-tsx")
        }

        @JvmStatic
        @Suppress("ktlint:standard:function-naming")
        private external fun tree_sitter_tsx(): Long
    }

    constructor() : super(tree_sitter_tsx())

    private constructor(ptr: Long) : super(ptr)

    override fun copy(): TSLanguage {
        return TreeSitterTsx(copyPtr())
    }
}
