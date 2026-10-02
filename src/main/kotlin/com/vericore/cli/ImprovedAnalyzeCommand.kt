package com.vericore.cli

import com.vericore.core.ai.AICodeAnalyzer
import com.vericore.core.cache.CacheManager
import com.vericore.core.config.ConfigLoader
import com.vericore.core.graph.RobustDependencyGraph
import com.vericore.core.intelligence.AnalysisSnapshotBuilder
import com.vericore.core.intelligence.EngineeringRiskEngine
import com.vericore.core.parser.ParsedFile
import com.vericore.core.scanner.OptimizedGitAnalyzer
import com.vericore.core.scanner.RepositoryScanner
import com.vericore.output.ReportGenerator
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ImprovedAnalyzeCommand :
    CliktCommand(name = "analyze", help = "Analyze a codebase and generate a report") {
    private val path by argument("path", help = "Path to analyze").default(".")
    private val noCache by option("--no-cache", help = "Disable caching").flag()
    private val clearCache by option("--clear-cache", help = "Clear cache before analyzing").flag()
    private val verbose by option("--verbose", "-v", help = "Enable verbose logging").flag()
    private val noSnapshot by option("--no-snapshot", help = "Do not write the machine-readable analysis snapshot").flag()

    override fun run() {
        echo("🚀 Starting Vericore analysis for: $path")
        val rootDir = File(path).canonicalFile
        if (!rootDir.exists()) {
            echo("❌ Error: Path does not exist: $path")
            return
        }
        if (!rootDir.isDirectory) {
            echo("❌ Error: Path is not a directory: $path")
            return
        }

        // Resolve project settings from the repository being analyzed.
        val config = ConfigLoader.loadForRepository(rootDir.path)
        val time = measureTimeMillis {
            try {
                if (clearCache) {
                    CacheManager().clear()
                    echo("🗑️  Cache cleared")
                }

                echo("📂 Scanning repository...")
                // RepositoryScanner resolves project configuration from rootDir itself.
                // This avoids accidentally passing configuration from another working directory.
                val scanner = RepositoryScanner()
                val files = scanner.scan(rootDir.path)
                echo("   Found ${files.size} files")

                if (files.isEmpty()) {
                    echo("❌ No source files found")
                    echo("   Supported extensions: .kt, .java")
                    return
                }
                if (files.size > config.maxFilesAnalyze) {
                    echo("⚠️  Too many files (${files.size}). Limit: ${config.maxFilesAnalyze}")
                    return
                }

                echo("🧠 Parsing code...")
                val cacheManager = if (config.enableCache && !noCache) CacheManager() else null
                val parsedFiles: List<ParsedFile> = try {
                    runBlocking { CodeParallelParser(cacheManager).parseFiles(files) }
                } catch (e: Exception) {
                    echo("❌ Parsing failed: ${e.message}")
                    if (verbose) println(e.stackTraceToString())
                    return
                }
                echo("   Parsed ${parsedFiles.size} files")
                val failedCount = files.size - parsedFiles.size
                if (failedCount > 0) echo("   ⚠️  $failedCount files failed to parse")

                echo("📜 Analyzing Git history...")
                val enrichedFiles = try {
                    OptimizedGitAnalyzer().analyze(rootDir.path, parsedFiles)
                } catch (e: Exception) {
                    echo("   ⚠️  Git analysis failed: ${e.message}")
                    if (verbose) println(e.stackTraceToString())
                    parsedFiles
                }

                echo("🕸️  Building dependency graph...")
                val graph = RobustDependencyGraph()
                val buildResult = graph.build(enrichedFiles)
                if (buildResult.isFailure) {
                    echo("❌ Failed to build graph: ${buildResult.exceptionOrNull()?.message}")
                    if (verbose) buildResult.exceptionOrNull()?.let { println(it.stackTraceToString()) }
                    return
                }
                val analyzeResult = graph.analyze()
                if (analyzeResult.isFailure) {
                    echo("❌ Failed to analyze graph: ${analyzeResult.exceptionOrNull()?.message}")
                    if (verbose) analyzeResult.exceptionOrNull()?.let { println(it.stackTraceToString()) }
                    return
                }

                val hotspots = graph.getTopHotspots(config.hotspotCount)
                echo("🗺️  Your Codebase Map")
                echo("├─ 🔥 Hot Zones (Top ${minOf(5, hotspots.size)}):")
                hotspots.take(5).forEachIndexed { index, (file, score) ->
                    val prefix = if (index == 4 || index == hotspots.lastIndex) "│   └─" else "│   ├─"
                    echo("$prefix ${File(file).name} (${String.format("%.4f", score)})")
                }

                val snapshot = AnalysisSnapshotBuilder.build(
                    repositoryPath = rootDir.path,
                    parsedFiles = enrichedFiles,
                    graph = graph.graph,
                    pageRankScores = graph.pageRankScores,
                    hasCycles = graph.hasCycles,
                    parseFailures = failedCount
                )
                val risks = EngineeringRiskEngine.calculate(snapshot)
                val highRiskCount = risks.count { it.level.name == "HIGH" || it.level.name == "CRITICAL" }
                echo("🛡️  Engineering risk: $highRiskCount high/critical files")

                // Reports belong to the repository being analyzed, not the CLI process cwd.
                val outputDir = rootDir.resolve("output")
                if (!outputDir.exists()) outputDir.mkdirs()
                if (!noSnapshot) {
                    val snapshotFile = File(outputDir, "analysis-snapshot.json")
                    snapshotFile.writeText(Json { prettyPrint = true }.encodeToString(snapshot))
                    echo("🧾 Analysis snapshot: ${snapshotFile.absolutePath}")

                    val riskFile = File(outputDir, "engineering-risks.json")
                    riskFile.writeText(Json { prettyPrint = true }.encodeToString(risks))
                    echo("🛡️  Risk report: ${riskFile.absolutePath}")
                }

                echo("📊 Generating report...")
                val reportFile = File(outputDir, "index.html")
                val learningPath = com.vericore.core.generator.LearningPathGenerator().generate(graph)
                ReportGenerator().generate(graph, reportFile.absolutePath, enrichedFiles, learningPath)
                echo("✅ Report: ${reportFile.absolutePath}")

                if (config.ai.enabled && config.ai.apiKey.isNotBlank()) {
                    echo("🤖 Generating AI Insights...")
                    val aiAnalyzer = AICodeAnalyzer(config.ai.apiKey, config.ai.model, config.ai.provider)
                    if (!aiAnalyzer.isConfigured()) {
                        echo("   ⚠️  AI is enabled but not properly configured")
                    } else {
                        try {
                            runBlocking {
                                val insights = aiAnalyzer.batchAnalyze(enrichedFiles, graph, limit = 10)
                                val aiReportFile = File(outputDir, "ai-insights.md")
                                aiReportFile.writeText("# AI Code Insights\n\n")
                                insights.forEach { (insightPath, insight) ->
                                    aiReportFile.appendText("## ${File(insightPath).name}\n")
                                    aiReportFile.appendText("**Purpose**: ${insight.purpose}\n\n")
                                    aiReportFile.appendText("**Complexity**: ${insight.complexity}/10\n")
                                    aiReportFile.appendText("**Refactoring Tips**: ${insight.refactoringTips.joinToString(", ")}\n\n")
                                }
                                echo("✨ AI Insights saved to: ${aiReportFile.absolutePath}")
                            }
                        } catch (e: Exception) {
                            echo("   ⚠️  AI analysis failed: ${e.message}")
                            if (verbose) println(e.stackTraceToString())
                        }
                    }
                }
            } catch (e: Exception) {
                echo("❌ Analysis failed: ${e.message}")
                if (verbose) println(e.stackTraceToString())
                throw e
            }
        }
        echo("✨ Complete in ${time}ms")
    }
}
