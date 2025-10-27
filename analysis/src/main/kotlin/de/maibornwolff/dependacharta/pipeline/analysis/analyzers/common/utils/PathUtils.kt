package de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.model.DirectImport
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.model.Import
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.model.RelativeImport
import de.maibornwolff.codegraph.pipeline.analysis.model.Path

/**
 * Resolves the path of an import relative to the file it is imported in.
 *
 * Example:
 * ```
 * import { myFunction } from "./myModule";
 * import { anotherFunction } from ../anotherModule;
 * ```
 * in a file located at /path/to/file.ts will resolve to
 * ```
 * /path/to/myModule and
 * /path/anotherModule respectively
 * ```
 *
 * @param import the import to resolve
 * @param filePath the path of the file the import is in
 * @return the resolved path
 */

fun resolveRelativePath(
    import: Import,
    filePath: Path
): Path {
    if (import is DirectImport) {
        return Path(import.directPath.split("/"))
    }
    var importParts = (import as RelativeImport).relativePath.split("/")
    val firstPathPart = importParts.first()
    if (firstPathPart == ".") {
        return Path(filePath.parts.dropLast(1) + importParts.drop(1))
    }
    var filePathParts = filePath.parts.dropLast(1)
    while (importParts.first() == "..") {
        importParts = importParts.drop(1)
        filePathParts = filePathParts.dropLast(1)
    }
    return Path(filePathParts + importParts)
}

fun Path.withoutFileSuffix(fileSuffix: String): Path {
    val suffixWithDot = ".$fileSuffix"
    this
        .getName()
        .endsWith(suffixWithDot)
        .let {
            val fileNameWithoutSuffix = if (it) this.parts.last().dropLast(suffixWithDot.length) else this.parts.last()
            return Path(this.parts.dropLast(1) + fileNameWithoutSuffix)
        }
}
