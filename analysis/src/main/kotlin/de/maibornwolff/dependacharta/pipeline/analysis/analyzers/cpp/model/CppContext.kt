package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.model

import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type

data class CppContext(
    val fileInfo: FileInfo,
    val includes: Set<Include>,
    private val dependencies: Set<Dependency>,
    val usedTypes: Set<Type>,
    val nameSpace: Path
) {
    val source: String
        get() = fileInfo.content

    fun addIncludes(newIncludes: List<Include>) = this.copy(includes = includes + newIncludes)

    fun addNamespace(newNamespace: List<String>) = this.copy(nameSpace = nameSpace + newNamespace)

    fun addDependencies(newDependency: Collection<Dependency>) = this.copy(dependencies = dependencies + newDependency)

    fun getDependencies(): Set<Dependency> = (dependencies + includes.map(Include::asDependency)).filter { !it.path.isEmpty() }.toSet()

    fun addUsedTypes(types: Set<Type>) = this.copy(usedTypes = usedTypes + types)

    companion object {
        fun empty(fileInfo: FileInfo) =
            CppContext(
                fileInfo,
                emptySet(),
                emptySet(),
                emptySet(),
                Path(emptyList())
            )
    }
}
