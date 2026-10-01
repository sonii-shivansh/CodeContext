package com.codecontext.cli

import com.codecontext.core.ai.CodebaseContext
import com.codecontext.core.ai.GeminiAskService
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
            throw com.github.ajalt.clikt.core.PrintHelpMessage(currentContext)
        }

        var config = ConfigLoader.loadEffective()
        if (!config.ai.enabled || config.ai.apiKey.isBlank()) {
            if (!AISetupPrompter.ensureConfigured(config.ai.model)) return
            config = ConfigLoader.loadEffective()
        }

        echo("🤖 Analyzing codebase to answer: \"$question\"")

        runBlocking {
            echo("   Gathering context...")
            val root = File(".")
            val scanner = RepositoryScanner()
            val files = scanner.scan(root.absolutePath)

            val cacheManager = com.codecontext.core.cache.CacheManager()
            val parallelParser = CodeParallelParser(cacheManager)
            val parsedFiles: List<com.codecontext.core.parser.ParsedFile> = parallelParser.parseFiles(files)

            val graph = RobustDependencyGraph()
            graph.build(parsedFiles)
            graph.analyze()

            val hotspots = graph.getTopHotspots(10).map { it.first }
            val context = CodebaseContext(
                totalFiles = parsedFiles.size,
                languages = listOf("Kotlin/Java"),
                hotspots = hotspots,
                recentChanges = emptyList()
            )

            try {
                val prompt = """
You are an expert guide for this codebase.

CODEBASE OVERVIEW:
- Total files: ${context.totalFiles}
- Languages: ${context.languages.joinToString(", ")}
- Top hotspots: ${context.hotspots.take(5).joinToString(", ") { File(it).name }}

DEVELOPER QUESTION: "${question.replace("\"", "\\\"")}"

Respond with JSON:
{
  "answer": "Clear, helpful answer (2-3 sentences)",
  "suggestedFiles": ["file1.kt", "file2.java"],
  "confidence": 0.0-1.0
}

Be concise and actionable. If you don't know, say so.
""".trimIndent()

                val response = if (config.ai.provider.equals("gemini", ignoreCase = true)) {
                    GeminiAskService(config.ai.apiKey, config.ai.model).ask(prompt)
                } else {
                    throw IllegalArgumentException("Interactive ask currently requires the Gemini provider")
                }

                echo("\n💡 ${response.answer}\n")
                if (response.suggestedFiles.isNotEmpty()) {
                    echo("📁 Check these files:")
                    response.suggestedFiles.forEach { echo("   - $it") }
                }
                echo("\n🎯 Confidence: ${(response.confidence * 100).toInt()}%")
            } catch (e: Exception) {
                if (e is com.codecontext.core.exceptions.CodeContextException) throw e
                val detail = e.message?.takeIf { it.isNotBlank() } ?: e::class.simpleName ?: "unknown error"
                throw com.codecontext.core.exceptions.AIProviderException("Failed to get AI response: $detail", e)
            }
        }
    }
}
