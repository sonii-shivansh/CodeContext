package com.codecontext.cli

// Verified import
import com.codecontext.core.ai.AICodeAnalyzer
import com.codecontext.core.cache.CacheManager
import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.parser.ParsedFile
import com.codecontext.core.scanner.OptimizedGitAnalyzer
import com.codecontext.core.scanner.RepositoryScanner
import com.codecontext.output.ReportGenerator
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking

class ImprovedAnalyzeCommand :
        CliktCommand(name = "analyze", help = "Analyze a codebase and generate a report") {
    private val path by argument("path", help = "Path to analyze").default(".")
    private val noCache by option("--no-cache", help = "Disable caching").flag()
    private val clearCache by option("--clear-cache", help = "Clear cache before analyzing").flag()

    override fun run() {
        echo("🚀 Starting CodeContext analysis for: $path")

        // Load config
        val config = ConfigLoader.load()

        val time = measureTimeMillis {
            try {
                // Clear cache if requested
                if (clearCache) {
                    CacheManager().clear()
                    echo("🗑️ Cache cleared")
                }

                // 1. Scan
                echo("📂 Scanning repository...")
                val scanner = RepositoryScanner()
                val files = scanner.scan(path)
                echo("   Found ${files.size} files")

                if (files.isEmpty()) {
                    echo("❌ No source files found")
                    return
                }

                if (files.size > config.maxFilesAnalyze) {
                    echo("⚠️ Too many files (${files.size}). Limit: ${config.maxFilesAnalyze}")
                    return
                }

                // 2. Parse (with caching and parallel processing)
                echo("🧠 Parsing code...")
                val cacheManager = if (config.enableCache && !noCache) CacheManager() else null
                val parser = CodeParallelParser(cacheManager)

                // Run suspending function in blocking context
                val parsedFiles: List<ParsedFile> = runBlocking { parser.parseFiles(files) }

                echo("   Parsed ${parsedFiles.size} files")

                // 3. Git Analysis (optimized)
                echo("📜 Analyzing Git history...")
                val gitAnalyzer = OptimizedGitAnalyzer()
                val enrichedFiles = gitAnalyzer.analyze(File(path).absolutePath, parsedFiles)

                // 4. Build Graph
                echo("🕸️ Building dependency graph...")
                val graph = RobustDependencyGraph()

                val buildResult = graph.build(enrichedFiles)
                if (buildResult.isFailure) {
                    echo("❌ Failed to build graph: ${buildResult.exceptionOrNull()?.message}")
                    return
                }

                val analyzeResult = graph.analyze()
                if (analyzeResult.isFailure) {
                    echo("❌ Failed to analyze graph: ${analyzeResult.exceptionOrNull()?.message}")
                    return
                }

                // Show hotspots
                val hotspots = graph.getTopHotspots(config.hotspotCount)
                echo("🗺️ Your Codebase Map")
                echo("├─ 🔥 Hot Zones (Top ${hotspots.size}):")
                hotspots.take(5).forEachIndexed { index, (file, score) ->
                    val prefix =
                            if (index == 4 || index == hotspots.lastIndex) "│   └─" else "│   ├─"
                    echo("$prefix ${File(file).name} (${String.format("%.4f", score)})")
                }

                // 5. Generate Report
                echo("📊 Generating report...")
                val outputDir = File("output")
                if (!outputDir.exists()) outputDir.mkdirs()
                val reportFile = File(outputDir, "index.html")

                val generator = ReportGenerator()
                val learningPath =
                        com.codecontext.core.generator.LearningPathGenerator().generate(graph)

                generator.generate(graph, reportFile.absolutePath, enrichedFiles, learningPath)

                echo("✅ Report: ${reportFile.absolutePath}")

                // 6. AI Analysis (Optional)
                if (config.ai.enabled && config.ai.apiKey.isNotBlank()) {
                    echo("🤖 Generating AI Insights (this may take a minute)...")
                    val aiAnalyzer = AICodeAnalyzer(config.ai.apiKey, config.ai.model)

                    runBlocking {
                        // Batch analyze top hotspots
                        val insights = aiAnalyzer.batchAnalyze(enrichedFiles, graph, limit = 10)

                        val aiReportFile = File(outputDir, "ai-insights.md")
                        aiReportFile.writeText("# AI Code Insights\n\n")

                        insights.forEach { (path, insight) ->
                            aiReportFile.appendText("## ${File(path).name}\n")
                            aiReportFile.appendText("**Purpose**: ${insight.purpose}\n\n")
                            aiReportFile.appendText("**Complexity**: ${insight.complexity}/10\n")
                            aiReportFile.appendText(
                                    "**Refactoring Tips**: ${insight.refactoringTips.joinToString(", ")}\n\n"
                            )
                        }

                        echo("✨ AI Insights saved to: ${aiReportFile.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                echo("❌ Analysis failed: ${e.message}")
                e.printStackTrace()
            }
        }

        echo("✨ Complete in ${time}ms")
    }
}
