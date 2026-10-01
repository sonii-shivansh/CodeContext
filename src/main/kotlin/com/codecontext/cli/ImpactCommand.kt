package com.codecontext.cli

import com.codecontext.cli.CodeParallelParser
import com.codecontext.core.cache.CacheManager
import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.intelligence.ChangeImpactEngine
import com.codecontext.core.scanner.OptimizedGitAnalyzer
import com.codecontext.core.scanner.RepositoryScanner
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ImpactCommand : CliktCommand(
    name = "impact",
    help = "Analyze the dependency impact of changed files"
) {
    private val path by argument("path", help = "Repository path")
    private val changed by argument("changed", help = "Changed file path(s) relative to the repository").multiple(required = true)
    private val jsonOutput by option("--json", help = "Write machine-readable impact JSON").flag()

    override fun run() {
        val root = File(path).absoluteFile.normalize()
        require(root.isDirectory) { "Repository path is not a directory: $path" }
        val config = ConfigLoader.load()
        val files = RepositoryScanner(config).scan(root.path)
        require(files.size <= config.maxFilesAnalyze) { "Repository exceeds the maximum file limit: ${config.maxFilesAnalyze}" }

        val parsed = runBlocking { CodeParallelParser(CacheManager()).parseFiles(files) }
        val enriched = OptimizedGitAnalyzer().analyze(root.path, parsed)
        val graph = RobustDependencyGraph()
        graph.build(enriched).getOrThrow()
        graph.analyze().getOrThrow()

        val pathLookup = enriched.associateBy { it.file.absolutePath.replace('\\', '/') }
        val changedAbsolute = changed.map { File(root, it).absolutePath.replace('\\', '/') }
        val packageByPath = pathLookup.mapValues { it.value.packageName }
        val churnByPath = pathLookup.mapValues { it.value.gitMetadata.changeFrequency }

        val result = ChangeImpactEngine.analyze(
            graph = graph.graph,
            changedPaths = changedAbsolute,
            pageRankScores = graph.pageRankScores,
            churnByPath = churnByPath,
            packageByPath = packageByPath
        )

        if (jsonOutput) {
            val output = File("output/change-impact.json")
            output.parentFile.mkdirs()
            output.writeText(Json { prettyPrint = true }.encodeToString(result))
            echo("🧭 Impact report: ${output.absolutePath}")
        }

        echo("🧭 Change Impact")
        echo("├─ Changed files: ${result.summary.changedFiles}")
        echo("├─ Impacted files: ${result.summary.impactedFiles}")
        echo("├─ Impacted packages: ${result.summary.impactedPackages}")
        echo("├─ Cross-package impacts: ${result.summary.crossPackageImpacts}")
        echo("├─ Test candidates: ${result.summary.testCandidates}")
        echo("└─ Maximum dependency depth: ${result.summary.maxDepth}")

        result.nodes.take(20).forEach { node ->
            echo("   ${node.relationship}: ${node.path} (depth=${node.depth}, score=${String.format("%.1f", node.score)})")
        }
    }
}
