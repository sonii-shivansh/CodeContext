package com.codecontext.cli

import com.codecontext.core.cache.CacheManager
import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.intelligence.ArchitectureDriftEngine
import com.codecontext.core.intelligence.ArchitectureIntelligenceEngine
import com.codecontext.core.intelligence.ArchitectureIntelligenceResult
import com.codecontext.core.scanner.RepositoryScanner
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class ArchitectureDriftCommand : CliktCommand(
    name = "architecture-drift",
    help = "Compare current architecture intelligence with a baseline snapshot"
) {
    private val path by argument("path", help = "Repository path")
    private val baseline by option("--baseline", help = "Baseline architecture JSON file").required()
    private val jsonOutput by option("--json", help = "Write machine-readable drift JSON").flag()

    override fun run() {
        val root = File(path).absoluteFile.normalize()
        require(root.isDirectory) { "Repository path is not a directory: $path" }
        val baselineFile = File(baseline).absoluteFile.normalize()
        require(baselineFile.isFile) { "Architecture baseline does not exist: $baseline" }

        val json = Json { ignoreUnknownKeys = false }
        val baselineResult = runCatching {
            json.decodeFromString<ArchitectureIntelligenceResult>(baselineFile.readText())
        }.getOrElse { error("Invalid architecture baseline: ${it.message}") }

        val config = ConfigLoader.load()
        val files = RepositoryScanner(config).scan(root.path)
        require(files.size <= config.maxFilesAnalyze) { "Repository exceeds the maximum file limit: ${config.maxFilesAnalyze}" }
        val parsed = runBlocking { CodeParallelParser(CacheManager()).parseFiles(files) }
        val graph = RobustDependencyGraph()
        graph.build(parsed).getOrThrow()
        graph.analyze().getOrThrow()
        val currentResult = ArchitectureIntelligenceEngine.analyze(graph.graph, root, config.architecture)
        val drift = ArchitectureDriftEngine.compare(baselineResult, currentResult)

        if (jsonOutput) {
            val output = File("output/architecture-drift.json")
            output.parentFile.mkdirs()
            output.writeText(Json { prettyPrint = true; encodeDefaults = true }.encodeToString(ArchitectureDriftResult.serializer(), drift))
            echo("🏛️ Architecture drift report: ${output.absolutePath}")
        }

        echo("🏛️ Architecture Drift")
        echo("├─ Added findings: ${drift.summary.addedFindings}")
        echo("├─ Removed findings: ${drift.summary.removedFindings}")
        echo("├─ New cycles: ${drift.summary.newCycles}")
        echo("├─ Removed cycles: ${drift.summary.removedCycles}")
        echo("└─ Changed layers: ${drift.summary.changedLayers}")
        drift.changes.take(20).forEach { change -> echo("   ${change.type}: ${change.key}") }
    }
}
