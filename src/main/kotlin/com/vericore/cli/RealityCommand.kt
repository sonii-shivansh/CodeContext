package com.vericore.cli

import com.vericore.core.config.ConfigLoader
import com.vericore.core.intelligence.AnalysisSnapshot
import com.vericore.core.intelligence.EngineeringContextEngine
import com.vericore.core.reality.EngineeringRealityEngine
import com.vericore.core.scanner.RepositoryScanner
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import kotlinx.serialization.json.Json

/** Builds one deterministic identity across the analysis and repository-state artifacts. */
class RealityCommand : CliktCommand(
    name = "reality",
    help = "Build a deterministic engineering-reality snapshot from the current repository state"
) {
    private val path by argument("path", help = "Repository path").default(".")
    private val jsonOutput by option("--json", help = "Write the machine-readable reality snapshot").flag()

    override fun run() {
        val root = File(path).canonicalFile
        require(root.isDirectory) { "Repository path is not a directory: ${root.path}" }

        val outputDir = root.resolve("output").apply { mkdirs() }
        val analysisFile = outputDir.resolve("analysis-snapshot.json")
        require(analysisFile.isFile) {
            "Analysis snapshot not found at ${analysisFile.path}. Run 'codecontext analyze ${root.path}' first."
        }

        val json = Json { ignoreUnknownKeys = false }
        val analysis = json.decodeFromString(AnalysisSnapshot.serializer(), analysisFile.readText())
        val context = EngineeringContextEngine.snapshot(
            root,
            RepositoryScanner(ConfigLoader.loadForRepository(root.path))
        )
        val reality = EngineeringRealityEngine.build(analysis, context)

        if (jsonOutput) {
            val output = outputDir.resolve("engineering-reality.json")
            output.writeText(EngineeringRealityEngine.encode(reality))
            echo("🧭 Engineering reality: ${output.path}")
        }

        echo("🧭 Engineering Reality")
        echo("├─ Commit: ${reality.repositoryCommit ?: "unknown"}")
        echo("├─ Files: ${reality.fileCount}")
        echo("├─ Graph: ${reality.graphNodes} nodes / ${reality.graphEdges} edges")
        echo("├─ Parse failures: ${reality.parseFailures}")
        echo("├─ Cycles: ${if (reality.hasCycles) "detected" else "none detected"}")
        echo("├─ Dirty: ${reality.dirty}")
        echo("└─ Reality digest: ${reality.realityDigest}")
    }
}
