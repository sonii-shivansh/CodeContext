package com.vericore.cli

import com.vericore.core.config.ConfigLoader
import com.vericore.core.intelligence.GitChangeSetBuilder
import com.vericore.core.intelligence.PRIntelligenceAnalyzer
import com.vericore.core.intelligence.PRIntelligenceResult
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
        val config = ConfigLoader.loadForRepository(root.path)
        val changeSet = if (base != null && head != null) {
            GitChangeSetBuilder.fromRevisions(root.path, base!!, head!!)
        } else {
            GitChangeSetBuilder.fromWorkingTree(root.path)
        }
        val result: PRIntelligenceResult = runBlocking {
            PRIntelligenceAnalyzer.analyze(root.path, changeSet, config)
        }

        if (jsonOutput) {
            val output = root.resolve("output/pr-intelligence.json")
            output.parentFile.mkdirs()
            output.writeText(Json { prettyPrint = true }.encodeToString(result))
            echo("🧭 PR Intelligence: ${output.absolutePath}")
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
}
