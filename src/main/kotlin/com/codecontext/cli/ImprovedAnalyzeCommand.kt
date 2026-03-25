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
import com.github.ajalt.clikt.parameters.options.int
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking

class ImprovedAnalyzeCommand :
        CliktCommand(name = "analyze", help = "Analyze a codebase and generate a report") {
    private val path by argument("path", help = "Path to analyze").default(".")
    private val noCache by option("--no-cache", help = "Disable caching").flag()
    private val clearCache by option("--clear-cache", help = "Clear cache before analyzing").flag()
    private val skipGit by option("--skip-git", help = "Skip git history analysis").flag()
    private val hotspotLimitOverride by
            option("--hotspots", help = "Override hotspot count shown in output").int()
    private val outputOverride by
            option("--output", help = "Output HTML report path (default: output/index.html)")

    // FIX: Add verbose mode for debugging
    private val verbose by option("--verbose", "-v", help = "Enable verbose logging").flag()

    override fun run() {
        echo("🚀 Starting CodeContext analysis for: $path")

        // FIX: Validate path before starting
        val rootDir = File(path)
        if (!rootDir.exists()) {
            echo("❌ Error: Path does not exist: $path")
            return
        }
        if (!rootDir.isDirectory) {
            echo("❌ Error: Path is not a directory: $path")
            return
        }
        if (hotspotLimitOverride != null && hotspotLimitOverride!! <= 0) {
            echo("❌ Error: --hotspots must be greater than 0")
            return
        }

        val config = ConfigLoader.load()
        val hotspotLimit = hotspotLimitOverride ?: config.hotspotCount
        val reportFile =
                outputOverride?.let { File(it) }
                        ?: File(File("output").apply { if (!exists()) mkdirs() }, "index.html")

        val time = measureTimeMillis {
            try {
                // Clear cache if requested
                if (clearCache) {
                    CacheManager().clear()
                    echo("🗑️  Cache cleared")
                }

                // 1. Scan
                echo("📂 Scanning repository...")
                val scanner = RepositoryScanner()
                val files = scanner.scan(path)
                echo("   Found ${files.size} files")

                if (files.isEmpty()) {
                    echo("❌ No source files found")
                    echo("   Supported extensions: .kt, .java")
                    echo("   Make sure you're in a source code directory")
                    return
                }

                if (files.size > config.maxFilesAnalyze) {
                    echo("⚠️  Too many files (${files.size}). Limit: ${config.maxFilesAnalyze}")
                    echo("   Increase maxFilesAnalyze in .codecontext.json to analyze more files")
                    return
                }

                // 2. Parse (with better error handling)
                echo("🧠 Parsing code...")
                val cacheManager = if (config.enableCache && !noCache) CacheManager() else null
                val parser = CodeParallelParser(cacheManager)

                val parsedFiles: List<ParsedFile> =
                        try {
                            runBlocking { parser.parseFiles(files) }
                        } catch (e: Exception) {
                            echo("❌ Parsing failed: ${e.message}")
                            if (verbose) println(e.stackTraceToString())
                            return
                        }

                echo("   Parsed ${parsedFiles.size} files")

                // FIX: Warn if many files failed to parse
                val failedCount = files.size - parsedFiles.size
                if (failedCount > 0) {
                    echo("   ⚠️  $failedCount files failed to parse (see logs above)")
                }

                // 3. Git Analysis (with error handling)
                val enrichedFiles =
                        if (skipGit) {
                            echo("📜 Skipping Git history analysis")
                            parsedFiles
                        } else {
                            echo("📜 Analyzing Git history...")
                            try {
                                val gitAnalyzer = OptimizedGitAnalyzer()
                                gitAnalyzer.analyze(File(path).absolutePath, parsedFiles)
                            } catch (e: Exception) {
                                echo("   ⚠️  Git analysis failed: ${e.message}")
                                if (verbose) println(e.stackTraceToString())
                                parsedFiles // Continue without git metadata
                            }
                        }

                // 4. Build Graph (with validation)
                echo("🕸️  Building dependency graph...")
                val graph = RobustDependencyGraph()

                val buildResult = graph.build(enrichedFiles)
                if (buildResult.isFailure) {
                    echo("❌ Failed to build graph: ${buildResult.exceptionOrNull()?.message}")
                    if (verbose)
                            buildResult.exceptionOrNull()?.let { println(it.stackTraceToString()) }
                    return
                }

                val analyzeResult = graph.analyze()
                if (analyzeResult.isFailure) {
                    echo("❌ Failed to analyze graph: ${analyzeResult.exceptionOrNull()?.message}")
                    if (verbose)
                            analyzeResult.exceptionOrNull()?.let {
                                println(it.stackTraceToString())
                            }
                    return
                }

                // Show hotspots
                val hotspots = graph.getTopHotspots(hotspotLimit)
                echo("🗺️  Your Codebase Map")
                echo("├─ 🔥 Hot Zones (Top ${minOf(5, hotspots.size)}):")
                hotspots.take(5).forEachIndexed { index, (file, score) ->
                    val prefix =
                            if (index == 4 || index == hotspots.lastIndex) "│   └─" else "│   ├─"
                    echo("$prefix ${File(file).name} (${String.format("%.4f", score)})")
                }

                // 5. Generate Report
                echo("📊 Generating report...")
                reportFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

                val generator = ReportGenerator()
                val learningPath =
                        com.codecontext.core.generator.LearningPathGenerator().generate(graph)

                generator.generate(graph, reportFile.absolutePath, enrichedFiles, learningPath)

                echo("✅ Report: ${reportFile.absolutePath}")

                // 6. AI Analysis (with proper error handling)
                if (config.ai.enabled && config.ai.apiKey.isNotBlank()) {
                    echo("🤖 Generating AI Insights...")

                    val aiAnalyzer =
                            AICodeAnalyzer(config.ai.apiKey, config.ai.model, config.ai.provider)

                    if (!aiAnalyzer.isConfigured()) {
                        echo("   ⚠️  AI is enabled but not properly configured")
                        echo("   Check your API key in .codecontext.json")
                    } else {
                        try {
                            runBlocking {
                                val insights =
                                        aiAnalyzer.batchAnalyze(enrichedFiles, graph, limit = 10)

                                val aiReportFile =
                                        File(reportFile.parentFile ?: File("."), "ai-insights.md")
                                aiReportFile.writeText("# AI Code Insights\n\n")

                                insights.forEach { (path, insight) ->
                                    aiReportFile.appendText("## ${File(path).name}\n")
                                    aiReportFile.appendText("**Purpose**: ${insight.purpose}\n\n")
                                    aiReportFile.appendText(
                                            "**Complexity**: ${insight.complexity}/10\n"
                                    )
                                    aiReportFile.appendText(
                                            "**Refactoring Tips**: ${insight.refactoringTips.joinToString(", ")}\n\n"
                                    )
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
                throw e // Re-throw for proper exit code
            }
        }

        echo("✨ Complete in ${time}ms")
    }
}
