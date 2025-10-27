package de.maibornwolff.dependacharta.pipeline.processing

import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.model.Node
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.processing.cycledetection.CycleAnalyzer
import de.maibornwolff.dependacharta.pipeline.processing.cycledetection.CycleAnalyzer.Companion.groupByLeafs
import de.maibornwolff.dependacharta.pipeline.processing.cycledetection.model.Cycle
import de.maibornwolff.dependacharta.pipeline.processing.cycledetection.model.NodeInformation
import de.maibornwolff.dependacharta.pipeline.processing.dependencies.DependencyResolverService
import de.maibornwolff.dependacharta.pipeline.processing.levelization.levelize
import de.maibornwolff.dependacharta.pipeline.processing.levelization.model.GraphNode
import de.maibornwolff.dependacharta.pipeline.processing.levelization.toGraphNodes
import de.maibornwolff.dependacharta.pipeline.processing.model.ProjectReportDto
import de.maibornwolff.dependacharta.pipeline.processing.reporting.ExportService
import de.maibornwolff.dependacharta.pipeline.processing.reporting.ReportService
import de.maibornwolff.dependacharta.pipeline.shared.Logger
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

private const val CG_JSON_FILE_TYPE = ".cg.json"

class ProcessingPipeline {
    companion object {
        fun run(
            outputFileName: String,
            outputDirectoryName: String,
            fileReports: List<FileReport>,
            omitGraphAnalysis: Boolean,
        ) {
            val reports = mergeIdenticalTypes(fileReports).let {
                if (it.isEmpty()) {
                    Logger.i("No identical types found. Continue with original file reports.")
                    return@let fileReports
                }
                it
            }
            val (resolvedNodes, nodeInfos) = resolveDependencies(reports)

            val cycles = detectCycles(nodeInfos, omitGraphAnalysis)
            val leveledNodes = levelize(resolvedNodes, omitGraphAnalysis)
            saveResults(reports, cycles, resolvedNodes, leveledNodes, outputFileName, outputDirectoryName)
        }

        private fun mergeIdenticalTypes(fileReports: List<FileReport>): List<FileReport> {
            val applicableLanguages = listOf(SupportedLanguage.CPP)

            val nodes = fileReports.flatMap { it.nodes }
            val (nodesValidForMerger, nodesNotValidForMerger) = nodes.partition { applicableLanguages.contains(it.language) }
            val pathWithNameToNode = nodesValidForMerger.groupBy { node -> node.pathWithName }
            val (potentialDuplicates, singles) = pathWithNameToNode.entries.partition { it.value.size > 1 }

            return listOf(FileReport(singles.flatMap { it.value } + mergeDuplicates(potentialDuplicates) + nodesNotValidForMerger))
        }

        private fun getFileNameWithoutExtension(node: Node): String = node.pathWithName.getName()

        private fun resolveDependencies(fileReports: List<FileReport>): Pair<Collection<Node>, Set<NodeInformation>> =
            Logger.timed("Resolving dependencies") {
                val resolvedNodes = DependencyResolverService.resolveNodes(fileReports)
                val nodeInfos = resolvedNodes.map { DependencyResolverService.mapNodeInfo(it) }.toSet()
                Pair(resolvedNodes, nodeInfos)
            }

        private fun detectCycles(
            nodeInfos: Set<NodeInformation>,
            omitGraphAnalysis: Boolean
        ): List<Cycle> {
            if (omitGraphAnalysis) {
                Logger.i("**** Cycle detection disabled by command line argument ")
                return emptyList()
            }

            return Logger.timed("Analyzing cycles") {
                val cycles = CycleAnalyzer.determineCycles(nodeInfos)
                Logger.i("Found a total of ${cycles.size} cycles")
                cycles
            }
        }

        private fun saveResults(
            fileReports: List<FileReport>,
            cycles: List<Cycle>,
            resolvedNodes: Collection<Node>,
            leveledNodes: List<GraphNode>,
            outputFileName: String,
            outputDirectoryName: String
        ): ProjectReportDto {
            Logger.i("Assembling project report from ${fileReports.size} files...")
            val cyclicEdgesPerLeaf = cycles.groupByLeafs()
            val report = ReportService.createProjectReport(resolvedNodes, cyclicEdgesPerLeaf, leveledNodes)
            val jsonExport = ExportService.toJson(report)
            saveToFile(
                fileName = outputFileName,
                content = jsonExport,
                outputPath = outputDirectoryName
            )
            Logger.i("Project report saved successfully as $outputFileName in directory '$outputDirectoryName'")

            return report
        }

        private fun levelize(
            resolvedNodes: Collection<Node>,
            omitGraphAnalysis: Boolean
        ): List<GraphNode> {
            if (omitGraphAnalysis) {
                Logger.i("**** Levelization disabled by command line argument ")
                return emptyList()
            }
            return Logger.timed("Leveling graph nodes") {
                val leveledNodes = levelize(resolvedNodes.toGraphNodes())
                leveledNodes
            }
        }

        private fun saveToFile(
            fileName: String,
            content: String,
            outputPath: String
        ) {
            val path = if (outputPath.endsWith("/").not()) "$outputPath/" else outputPath
            Path(path).createDirectories()
            File(path + fileName + CG_JSON_FILE_TYPE).printWriter().use { out ->
                out.println(content)
            }
        }

        private fun mergeDuplicates(potentialDuplicates: List<Map.Entry<Path, List<Node>>>): List<Node> =
            potentialDuplicates.flatMap { pd ->
                val nodesByName = pd.value.groupBy { node -> getFileNameWithoutExtension(node) }
                val (duplicates, nonDuplicates) = nodesByName.entries.partition { it.value.size > 1 }

                val merged = duplicates
                    .flatMap { it.value }
                    .reduce { a, b ->

                        val fileEndingA = a.physicalPath.split(".").lastOrNull() ?: ""
                        val fileEndingB = b.physicalPath.split(".").lastOrNull() ?: ""

                        a.copy(
                            physicalPath = "${pd.key.with("/")}.$fileEndingA/.$fileEndingB",
                            dependencies = a.dependencies + b.dependencies,
                            usedTypes = a.usedTypes + b.usedTypes,
                            resolvedNodeDependencies = a.resolvedNodeDependencies + b.resolvedNodeDependencies
                        )
                    }

                nonDuplicates.flatMap { it.value } + merged
            }
    }
}
