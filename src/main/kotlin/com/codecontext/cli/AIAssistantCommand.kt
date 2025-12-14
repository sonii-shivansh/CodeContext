package com.codecontext.cli

import com.codecontext.core.ai.AICodeAnalyzer
import com.codecontext.core.ai.CodebaseContext
import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.scanner.RepositoryScanner
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import java.io.File
import kotlinx.coroutines.runBlocking

class AIAssistantCommand :
        CliktCommand(name = "ask", help = "Ask questions about your codebase using AI") {
    private val question by argument("question", help = "The question to ask").default("")

    override fun run() {
        if (question.isBlank()) {
            echo("❌ Please provide a question. Usage: codecontext ask \"Where is auth logic?\"")
            return
        }

        val config = ConfigLoader.load()

        if (!config.ai.enabled || config.ai.apiKey.isEmpty()) {
            echo(
                    "❌ AI features disabled. Please enable them in .codecontext.json and set output.ai.apiKey"
            )
            echo("Example config:")
            echo(
                    """
                {
                  "ai": {
                    "enabled": true,
                    "apiKey": "sk-ant-...",
                    "provider": "anthropic"
                  }
                }
            """.trimIndent()
            )
            return
        }

        echo("🤖 Analyzing codebase to answer: \"$question\"")

        runBlocking {
            try {
                // Quick Scan & Parse (We need context)
                // In a real optimized version, we would load from checks/cache directly without
                // reparsing if possible
                // For now, we reuse the fast pipeline
                echo("   Gathering context...")
                val root = File(".")
                val scanner = RepositoryScanner()
                val files = scanner.scan(root.absolutePath)

                // Re-instantiate cache manager or load config-based one
                val cacheManager = com.codecontext.core.cache.CacheManager()
                val parallelParser = CodeParallelParser(cacheManager)
                val parsedFiles: List<com.codecontext.core.parser.ParsedFile> =
                        parallelParser.parseFiles(files) // This uses cache, so it's fast

                // Build Graph for Hotspots
                val graph = RobustDependencyGraph()
                graph.build(parsedFiles)
                graph.analyze() // PageRank

                val hotspots = graph.getTopHotspots(10).map { it.first }

                // Gather minimal git context (skipping full log for speed in 'ask', or rely on
                // cache)
                // For 'ask', maybe just file list basics + hotspots is enough

                val context =
                        CodebaseContext(
                                totalFiles = parsedFiles.size,
                                languages = listOf("Kotlin/Java"), // Detect properly if needed
                                hotspots = hotspots,
                                recentChanges = emptyList() // Implement if we want 'recent changes'
                                // context, usually slow
                                )

                val aiAnalyzer = AICodeAnalyzer(config.ai.apiKey, config.ai.model)
                val response = aiAnalyzer.askQuestion(question, context)

                echo("\n💡 ${response.answer}\n")

                if (response.suggestedFiles.isNotEmpty()) {
                    echo("📁 Check these files:")
                    response.suggestedFiles.forEach { echo("   - $it") }
                }

                echo("\n🎯 Confidence: ${(response.confidence * 100).toInt()}%")
            } catch (e: Exception) {
                echo("❌ Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
