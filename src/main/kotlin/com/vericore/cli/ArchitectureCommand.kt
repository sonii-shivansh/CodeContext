package com.vericore.cli

import com.vericore.core.cache.CacheManager
import com.vericore.core.config.ConfigLoader
import com.vericore.core.graph.RobustDependencyGraph
import com.vericore.core.intelligence.ArchitectureIntelligenceEngine
import com.vericore.core.scanner.RepositoryScanner
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ArchitectureCommand : CliktCommand(name = "architecture", help = "Analyze repository architecture and detect structural violations") {
    private val path by argument("path", help = "Repository path")
    private val jsonOutput by option("--json", help = "Write machine-readable architecture JSON").flag()

    override fun run() {
        val root = File(path).absoluteFile.normalize()
        require(root.isDirectory) { "Repository path is not a directory: $path" }
        val config = ConfigLoader.load()
        val files = RepositoryScanner(config).scan(root.path)
        require(files.size <= config.maxFilesAnalyze) { "Repository exceeds the maximum file limit: ${config.maxFilesAnalyze}" }
        val parsed = runBlocking { CodeParallelParser(CacheManager()).parseFiles(files) }
        val graph = RobustDependencyGraph()
        graph.build(parsed).getOrThrow()
        graph.analyze().getOrThrow()
        val result = ArchitectureIntelligenceEngine.analyze(graph.graph, root, config.architecture)
        if (jsonOutput) {
            val output = File("output/architecture.json")
            output.parentFile.mkdirs()
            output.writeText(Json { prettyPrint = true; encodeDefaults = true }.encodeToString(result))
            echo("🏛️ Architecture report: ${output.absolutePath}")
        }
        echo("🏛️ Architecture Intelligence")
        echo("├─ Files analyzed: ${result.summary.filesAnalyzed}")
        echo("├─ Dependency edges: ${result.summary.dependencyEdges}")
        echo("├─ Findings: ${result.summary.findings}")
        echo("├─ Cycles: ${result.summary.cycles}")
        echo("├─ Cross-layer dependencies: ${result.summary.crossLayerDependencies}")
        echo("└─ High-coupling files: ${result.summary.highCouplingFiles}")
        result.findings.take(20).forEach { finding ->
            echo("   ${finding.severity} ${finding.ruleId}: ${finding.source}${finding.target?.let { " -> $it" } ?: ""}")
        }
    }
}
