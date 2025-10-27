package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.model

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.model.Import
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.common.utils.resolveRelativePath
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp.transformFileEnding
import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency
import de.maibornwolff.codegraph.pipeline.analysis.model.Path

data class Include(
    val includeFile: Import,
    val containingFilePath: Path
) {
    fun asDependency(): Dependency {
        val path = resolveRelativePath(includeFile, containingFilePath)
        val transformedFilename = path.parts.last().transformFileEnding()
        return Dependency(Path(path.parts.dropLast(1) + transformedFilename), isWildcard = false)
    }
}
