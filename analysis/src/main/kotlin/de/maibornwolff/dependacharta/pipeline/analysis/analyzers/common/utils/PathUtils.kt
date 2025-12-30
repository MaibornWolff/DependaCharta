package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.utils

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.DirectImport
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.Import
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.common.model.RelativeImport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path

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

/**
 * Strips known source file extensions from import paths to normalize them.
 * This ensures dependency paths match node paths which have extensions stripped.
 *
 * @return The path with the extension removed if it was a known source extension
 */
fun String.stripSourceFileExtension(): String {
    return when {
        this.endsWith(".vue") -> this.dropLast(4)
        this.endsWith(".tsx") -> this.dropLast(4)
        this.endsWith(".jsx") -> this.dropLast(4)
        this.endsWith(".ts") -> this.dropLast(3)
        this.endsWith(".js") -> this.dropLast(3)
        else -> this
    }
}
