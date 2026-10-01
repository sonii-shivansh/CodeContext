package com.codecontext.cli

import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.intelligence.ArchitectureContract
import com.codecontext.core.intelligence.ArchitectureContractEngine
import com.codecontext.core.intelligence.ArchitectureIntelligenceEngine
import com.codecontext.core.scanner.RepositoryScanner
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import kotlinx.serialization.json.Json

class ArchitectureContractCommand : CliktCommand(
    name = "architecture-contract",
    help = "Evaluate repository architecture against a deterministic contract"
) {
    private val path by argument("path", help = "Repository path")
    private val contractPath by option("--contract", help = "Architecture contract JSON; defaults to .codecontext-architecture-contract.json")
    private val jsonOutput by option("--json", help = "Write machine-readable contract result").flag()

    override fun run() {
        val root = File(path).absoluteFile.normalize()
        val config = ConfigLoader.load()
        val scanner = RepositoryScanner(config)
        val files = scanner.scan(root)
        val graph = RobustDependencyGraph(files, root, config).apply { analyze().getOrThrow() }
        val architecture = ArchitectureIntelligenceEngine.analyze(graph.graph, root, config.architecture)
        val json = Json { ignoreUnknownKeys = false; prettyPrint = true }
        val file = File(contractPath ?: File(root, ".codecontext-architecture-contract.json").path)
        val contract = if (file.exists()) {
            json.decodeFromString<ArchitectureContract>(file.readText())
        } else {
            ArchitectureContract()
        }
        val result = ArchitectureContractEngine.evaluate(architecture, contract)
        if (jsonOutput) {
            val output = File("output/architecture-contract.json")
            output.parentFile.mkdirs()
            output.writeText(json.encodeToString(ArchitectureContractResult.serializer(), result))
            echo("🛡️ Architecture contract: ${output.absolutePath}")
        }
        echo("🛡️ Architecture Contract")
        echo("├─ Passed: ${result.passed}")
        echo("├─ Findings: ${architecture.findings.size}")
        echo("├─ Cycles: ${architecture.cycles.size}")
        echo("└─ Violations: ${result.violations.size}")
        if (!result.passed) {
            result.violations.take(30).forEach { violation ->
                echo("   ${violation.ruleId}: ${violation.message}")
            }
            throw IllegalStateException("Architecture contract failed")
        }
    }
}
