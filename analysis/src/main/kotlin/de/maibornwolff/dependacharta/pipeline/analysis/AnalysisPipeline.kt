package de.maibornwolff.dependacharta.pipeline.analysis

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.LanguageAnalyzerFactory
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileReport
import de.maibornwolff.dependacharta.pipeline.analysis.synchronization.AnalysisRecord
import de.maibornwolff.dependacharta.pipeline.analysis.synchronization.AnalysisSynchronizer
import de.maibornwolff.dependacharta.pipeline.analysis.synchronization.RootDirectoryWalker
import de.maibornwolff.dependacharta.pipeline.shared.Logger
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import java.io.File
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class AnalysisPipeline {
    companion object {
        fun run(
            rootDirectory: String,
            clean: Boolean,
            languages: List<SupportedLanguage>,
            maxConcurrency: Int = Runtime.getRuntime().availableProcessors()
        ): List<FileReport> =
            Logger.timed("Executing Analysis") {
                runBlocking {
                    val rootWalker = RootDirectoryWalker(File(rootDirectory), languages)
                    val analysisSynchronizer = AnalysisSynchronizer()

                    if (clean) {
                        cleanTempFiles()
                    }

                    val analysisRecord = fetchOrCreateAnalysisRecord(rootWalker, analysisSynchronizer)
                    analyzeFilesParallel(rootWalker, analysisSynchronizer, analysisRecord, maxConcurrency)
                    val finalRecord = fetchOrCreateAnalysisRecord(rootWalker, analysisSynchronizer)
                    return@runBlocking finalRecord.pathToFileReport
                        .filter { it.value != null }
                        .map { analysisSynchronizer.readFileReport(it.value!!) }
                }
            }

        fun cleanTempFiles() {
            Logger.i("Deleting temporary analysis files...")
            AnalysisSynchronizer().deleteTempFiles()
        }

        private suspend fun analyzeFilesParallel(
            rootWalker: RootDirectoryWalker,
            analysisSynchronizer: AnalysisSynchronizer,
            analysisRecord: AnalysisRecord,
            maxConcurrency: Int
        ) = coroutineScope {
            val tempRecord = ConcurrentHashMap<String, String>()

            analysisRecord.pathToFileReport.forEach { (path, id) ->
                if (id != null) {
                    tempRecord[path] = id
                }
            }

            val allFiles = analysisRecord.pathToFileReport.keys
            val filesToProcess = analysisRecord.pathToFileReport.filterValues { it == null }.keys
            val alreadyProcessed = allFiles.size - filesToProcess.size
            var nonFatalAnalysisExceptionsHappened = false

            println("Processing ${filesToProcess.size} files with $maxConcurrency threads...")

            createProgressbar(allFiles, alreadyProcessed.toLong()).use { progressBar ->
                val semaphore = Semaphore(maxConcurrency)

                filesToProcess
                    .map { filePath ->
                        async {
                            semaphore.withPermit {
                                processFile(
                                    filePath,
                                    rootWalker,
                                    analysisSynchronizer,
                                    tempRecord,
                                    progressBar,
                                    analysisRecord // <- hier übergeben
                                )
                            }
                        }
                    }.awaitAll()

                val finalRecord = analysisRecord.pathToFileReport.toMutableMap()
                tempRecord.forEach { (path, id) ->
                    finalRecord[path] = id
                }
                analysisSynchronizer.saveAnalysisRecord(AnalysisRecord(finalRecord))
            }
        }

        private suspend fun processFile(
            filePath: String,
            rootWalker: RootDirectoryWalker,
            analysisSynchronizer: AnalysisSynchronizer,
            tempRecord: ConcurrentHashMap<String, String>,
            progressBar: ProgressBar,
            analysisRecord: AnalysisRecord
        ) {
            val fileInfo = rootWalker.getFileInfo(filePath)
            var nonFatalAnalysisExceptionsHappened = false

            try {
                val fileReport = withContext(Dispatchers.IO) {
                    LanguageAnalyzerFactory.createAnalyzer(fileInfo).analyze()
                }

                val id = analysisSynchronizer.saveFileReport(fileReport)
                tempRecord[filePath] = id

                if (tempRecord.size % 50 == 0) {
                    val recordToSave = analysisRecord.pathToFileReport.toMutableMap()
                    tempRecord.forEach { (path, id) -> recordToSave[path] = id }
                    analysisSynchronizer.saveAnalysisRecord(AnalysisRecord(recordToSave))
                }

                synchronized(progressBar) {
                    progressBar.step()
                }
            } catch (ex: Exception) {
                nonFatalAnalysisExceptionsHappened = true
                Logger.d("${fileInfo.physicalPath}: ${ex.message}")
                synchronized(progressBar) {
                    progressBar.step()
                }
            }

            if (nonFatalAnalysisExceptionsHappened) {
                Logger.w("Non-fatal analysis exceptions happened, please check the log file for details or re-run with '-l debug'")
            }
        }

        private fun fetchOrCreateAnalysisRecord(
            rootWalker: RootDirectoryWalker,
            analysisSynchronizer: AnalysisSynchronizer,
        ): AnalysisRecord {
            val analysisRecord = if (analysisSynchronizer.hasOngoingAnalysis()) {
                analysisSynchronizer.getAnalysisRecord()
            } else {
                val record = AnalysisRecord(rootWalker.walk().associateWith { null })
                analysisSynchronizer.saveAnalysisRecord(record)
                record
            }
            return analysisRecord
        }

        private fun createProgressbar(
            progressUnit: Collection<Any>,
            processedUnits: Long
        ): ProgressBar =
            ProgressBarBuilder()
                .setStyle(ProgressBarStyle.ASCII)
                .startsFrom(processedUnits, Duration.ZERO)
                .setInitialMax(progressUnit.count().toLong())
                .setTaskName("Analyze")
                .build()
    }
}