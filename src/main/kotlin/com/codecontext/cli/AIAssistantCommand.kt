package com.codecontext.cli

import com.codecontext.core.ai.CodebaseContext
import com.codecontext.core.ai.GeminiQuestionService
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
                val response = if (config.ai.provider.equals("gemini", ignoreCase = true)) {
                    GeminiQuestionService(config.ai.apiKey, config.ai.model).ask(question, context)
                } else {
                    throw IllegalArgumentException("Unsupported AI provider for ask: ${config.ai.provider}")
                }

                echo("\n💡 ${response.answer}\n")
                if (response.suggestedFiles.isNotEmpty()) {
                    echo("📁 Check these files:")
                    response.suggestedFiles.forEach { echo("   - $it") }
                }
                echo("\n🎯 Confidence: ${(response.confidence * 100).toInt()}%")
            } catch (e: Exception) {
                if (e is com.codecontext.core.exceptions.CodeContextException) throw e
                val detail = e.message?.takeIf { it.isNotBlank() } ?: e::class.simpleName.orEmpty()
                throw com.codecontext.core.exceptions.AIProviderException("Failed to get AI response: $detail", e)
            }
        }
    }
}
