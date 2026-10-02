package com.vericore.core.intelligence

import com.vericore.cli.CodeParallelParser
import com.vericore.core.cache.CacheManager
import com.vericore.core.config.CodeContextConfig
import com.vericore.core.config.ConfigLoader
import com.vericore.core.graph.RobustDependencyGraph
import com.vericore.core.scanner.OptimizedGitAnalyzer
import com.vericore.core.scanner.RepositoryScanner
import java.io.File

/** Orchestrates existing deterministic analyzers into one PR Intelligence result. */
object PRIntelligenceAnalyzer {
    suspend fun analyze(
        repoPath: String,
        changeSet: ChangeSet,
        config: CodeContextConfig = ConfigLoader.load()
    ): PRIntelligenceResult {
        val root = File(repoPath).canonicalFile
        require(root.isDirectory) { "Repository path is not a directory: $repoPath" }
        val files = RepositoryScanner(config).scan(root.path)
        require(files.size <= config.maxFilesAnalyze) { "Repository exceeds the maximum file limit: ${config.maxFilesAnalyze}" }
        val parsed = CodeParallelParser(CacheManager()).parseFiles(files)
        val enriched = OptimizedGitAnalyzer().analyze(root.path, parsed)
        val graph = RobustDependencyGraph()
        graph.build(enriched).getOrThrow()
        graph.analyze().getOrThrow()

        val absoluteByRelative = enriched.associate { file -> relative(root, file.file.absolutePath) to file.file.absolutePath.replace('\\', '/') }
        val changedAbsolute = changeSet.files.mapNotNull { absoluteByRelative[relative(root, it.path)] }
        val packageByRelative = enriched.associate { file -> relative(root, file.file.absolutePath) to file.packageName }
        val churnByAbsolute = enriched.associate { it.file.absolutePath.replace('\\', '/') to it.gitMetadata.changeFrequency }
        val packageByAbsolute = enriched.associate { it.file.absolutePath.replace('\\', '/') to it.packageName }

        val impact = ChangeImpactEngine.analyze(
            graph = graph.graph,
            changedPaths = changedAbsolute,
            pageRankScores = graph.pageRankScores,
            churnByPath = churnByAbsolute,
            packageByPath = packageByAbsolute
        )
        val snapshot = AnalysisSnapshotBuilder.build(
            repositoryPath = root.path,
            parsedFiles = enriched,
            graph = graph.graph,
            pageRankScores = graph.pageRankScores,
            hasCycles = graph.hasCycles
        )
        val risks = EngineeringRiskEngine.calculate(snapshot)
        val tests = impact.nodes.filter { it.relationship == ImpactRelationship.TEST_CANDIDATE }.map { relative(root, it.path) }
        return PRIntelligenceEngine.analyze(
            changeSet = changeSet,
            impact = impact,
            risks = risks,
            packageByPath = packageByRelative,
            testCandidates = tests,
            pathMapper = { relative(root, it) }
        )
    }

    private fun relative(root: File, path: String): String = runCatching {
        root.toPath().relativize(File(path).toPath()).toString().replace('\\', '/')
    }.getOrDefault(path.replace('\\', '/'))
}
