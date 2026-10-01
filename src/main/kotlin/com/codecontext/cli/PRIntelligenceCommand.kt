package com.codecontext.cli

import com.codecontext.core.cache.CacheManager
import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.intelligence.AnalysisSnapshotBuilder
import com.codecontext.core.intelligence.ChangeImpactEngine
import com.codecontext.core.intelligence.GitChangeSetBuilder
import com.codecontext.core.intelligence.PRIntelligenceEngine
import com.codecontext.core.intelligence.PRIntelligenceResult
import com.codecontext.core.scanner.OptimizedGitAnalyzer
import com.codecontext.core.scanner.RepositoryScanner
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PRIntelligenceCommand : CliktCommand(
    name = "pr-intelligence",
    help = "Analyze a Git change set for impact, risk, architecture, and test signals"
) {
    private val path by argument("path", help = "Local Git repository path")
    private val base by option("--base", help = "Base Git revision; pair with --head")
    private val head by option("--head", help = "Head Git revision; pair with --base")
    private val jsonOutput by option("--json", help = "Write machine-readable JSON to output/pr-intelligence.json").flag()

    override fun run() {
        require((base == null) == (head == null)) { "--base and --head must be supplied together" }
        val root = File(path).canonicalFile
        require(root.isDirectory) { "Repository path is not a directory: $path" }
        val config = ConfigLoader.load()
        val files = RepositoryScanner(config).scan(root.path)
        require(files.size <= config.maxFilesAnalyze) { "Repository exceeds the maximum file limit: ${config.maxFilesAnalyze}" }

        val parsed = runBlocking { CodeParallelParser(CacheManager()).parseFiles(files) }
        val enriched = OptimizedGitAnalyzer().analyze(root.path, parsed)
        val graph = RobustDependencyGraph()
        graph.build(enriched).getOrThrow()
        graph.analyze().getOrThrow()

        val changeSet = if (base != null && head != null) {
            GitChangeSetBuilder.fromRevisions(root.path, base!!, head!!)
        } else {
            GitChangeSetBuilder.fromWorkingTree(root.path)
        }

        val absoluteByRelative = enriched.associate { file ->
            relative(root, file.file.absolutePath) to file.file.absolutePath.replace('\\', '/')
        }
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
        val risks = com.codecontext.core.intelligence.EngineeringRiskEngine.calculate(snapshot)
        val tests = impact.nodes.filter { it.relationship.name == "TEST_CANDIDATE" }.map { relative(root, it.path) }
        val result: PRIntelligenceResult = PRIntelligenceEngine.analyze(
            changeSet = changeSet,
            impact = impact,
            risks = risks,
            packageByPath = packageByRelative,
            testCandidates = tests,
            pathMapper = { relative(root, it) }
        )

        if (jsonOutput) {
            val output = File("output/pr-intelligence.json")
            output.parentFile.mkdirs()
            output.writeText(Json { prettyPrint = true }.encodeToString(result))
            echo("🧭 PR Intelligence: ${output.path}")
        }

        echo("🧭 PR Intelligence")
        echo("├─ Changed files: ${result.changeSummary.filesChanged}")
        echo("├─ Lines: +${result.changeSummary.additions} / -${result.changeSummary.deletions}")
        echo("├─ Impacted files: ${result.impactedFiles}")
        echo("├─ Cross-package impacts: ${result.crossPackageImpacts}")
        echo("├─ Test candidates: ${result.testCandidates.size}")
        echo("└─ Aggregate severity: ${result.aggregateSeverity}")
        result.findings.forEach { finding ->
            echo("   [${finding.severity}] ${finding.ruleId}: ${finding.reason}")
        }
    }

    private fun relative(root: File, path: String): String = runCatching {
        root.toPath().relativize(File(path).toPath()).invariantSeparatorsPath
    }.getOrDefault(path.replace('\\', '/'))
}
