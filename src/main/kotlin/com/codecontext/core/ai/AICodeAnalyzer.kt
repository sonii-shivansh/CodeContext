package com.codecontext.core.ai

import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.parser.ParsedFile
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class AIInsight(
        val file: String,
        val purpose: String,
        val complexity: Int, // 1-10
        val keyComponents: List<String>,
        val refactoringTips: List<String>,
        val securityConcerns: List<String>,
        val businessImpact: String,
        val readingTime: Int // minutes
)

@Serializable
data class AIConversationResponse(
        val answer: String,
        val suggestedFiles: List<String>,
        val confidence: Double
)

class AICodeAnalyzer(
        private val apiKey: String,
        private val model: String = "claude-sonnet-4-20250514"
) {
        private val client = HttpClient.newHttpClient()
        private val json = Json { ignoreUnknownKeys = true }

        /** Analyze a single file and generate comprehensive insights */
        suspend fun analyzeFile(file: ParsedFile, context: AnalysisContext): AIInsight =
                withContext(Dispatchers.IO) {
                        val prompt = buildFileAnalysisPrompt(file, context)
                        val response = callClaude(prompt)

                        return@withContext parseInsight(response, file.file.absolutePath)
                }

        /** Batch analyze multiple files efficiently */
        suspend fun batchAnalyze(
                files: List<ParsedFile>,
                graph: RobustDependencyGraph,
                limit: Int = 50
        ): Map<String, AIInsight> = coroutineScope {

                // Prioritize high-impact files (high PageRank)
                val prioritized =
                        files
                                .sortedByDescending {
                                        graph.pageRankScores[it.file.absolutePath] ?: 0.0
                                }
                                .take(limit)

                println("🤖 AI analyzing top $limit files...")

                // Process in chunks to respect rate limits
                prioritized
                        .chunked(5)
                        .flatMap { chunk ->
                                chunk
                                        .map { file ->
                                                async {
                                                        val context =
                                                                AnalysisContext(
                                                                        totalFiles = files.size,
                                                                        dependencies =
                                                                                graph.graph
                                                                                        .outDegreeOf(
                                                                                                file.file
                                                                                                        .absolutePath
                                                                                        ),
                                                                        dependents =
                                                                                graph.graph
                                                                                        .inDegreeOf(
                                                                                                file.file
                                                                                                        .absolutePath
                                                                                        ),
                                                                        pageRank =
                                                                                graph.pageRankScores[
                                                                                        file.file
                                                                                                .absolutePath]
                                                                                        ?: 0.0,
                                                                        gitChurn =
                                                                                file.gitMetadata
                                                                                        .changeFrequency
                                                                )

                                                        try {
                                                                file.file.absolutePath to
                                                                        analyzeFile(file, context)
                                                        } catch (e: Exception) {
                                                                println(
                                                                        "⚠️ AI analysis failed for ${file.file.name}: ${e.message}"
                                                                )
                                                                null
                                                        }
                                                }
                                        }
                                        .awaitAll()
                                        .filterNotNull()
                        }
                        .toMap()
        }

        /** Conversational interface - answer developer questions */
        suspend fun askQuestion(
                question: String,
                codebaseContext: CodebaseContext
        ): AIConversationResponse =
                withContext(Dispatchers.IO) {
                        val prompt = buildConversationPrompt(question, codebaseContext)
                        val response = callClaude(prompt)

                        return@withContext parseConversation(response)
                }

        /** Generate "Why is this important?" explanations for hotspots */
        suspend fun explainHotspot(
                file: String,
                pageRank: Double,
                gitChurn: Int,
                dependents: Int
        ): String =
                withContext(Dispatchers.IO) {
                        val prompt =
                                """
        Explain in 2-3 sentences why this file is critical to the codebase:
        
        File: ${File(file).name}
        PageRank Score: $pageRank (higher = more central)
        Git Changes: $gitChurn times
        Files Depending On It: $dependents
        
        Write for a developer joining the team. Focus on impact and risks.
        """

                        callClaude(prompt).trim()
                }

        /** Suggest learning order based on complexity and dependencies */
        suspend fun generateLearningStrategy(
                files: List<ParsedFile>,
                developerLevel: String // "junior", "mid", "senior"
        ): String =
                withContext(Dispatchers.IO) {
                        val filesSummary =
                                files.take(20).joinToString("\n") { file ->
                                        "- ${file.file.name} (package: ${file.packageName}, imports: ${file.imports.size})"
                                }

                        val prompt =
                                """
        You're onboarding a $developerLevel developer to a codebase.
        
        Here are the key files:
        $filesSummary
        
        Create a 7-day learning plan with:
        - Day-by-day file reading order
        - What to focus on in each file
        - Hands-on exercises (e.g., "Add a test", "Trace this function call")
        
        Format as Markdown.
        """

                        callClaude(prompt)
                }

        private fun buildFileAnalysisPrompt(file: ParsedFile, context: AnalysisContext): String {
                val fileContent =
                        try {
                                // In real app, we should read file content.
                                // But ParsedFile only has path? No, it has file object.
                                file.file.readText().take(3000) // First 3000 chars
                        } catch (e: Exception) {
                                "[File content unavailable]"
                        }

                return """
        Analyze this codebase file and provide structured insights.
        
        FILE: ${file.file.name}
        PACKAGE: ${file.packageName}
        IMPORTS: ${file.imports.take(10).joinToString(", ")}
        
        CONTEXT:
        - Total codebase size: ${context.totalFiles} files
        - This file depends on: ${context.dependencies} files
        - This file is used by: ${context.dependents} files
        - PageRank (importance): ${String.format("%.4f", context.pageRank)}
        - Git churn: ${context.gitChurn} changes
        
        CODE PREVIEW:
        ```
        ${fileContent.replace("```", "")}
        ```
        
        Respond ONLY with JSON:
        {
          "purpose": "One sentence: what does this file do?",
          "complexity": 1-10 rating,
          "keyComponents": ["component1", "component2"],
          "refactoringTips": ["tip1", "tip2"],
          "securityConcerns": ["concern1"] or [],
          "businessImpact": "Why this matters to the product",
          "readingTime": estimated minutes to understand
        }
        """
        }

        private fun buildConversationPrompt(question: String, context: CodebaseContext): String {
                return """
        You're an expert guide for this codebase.
        
        CODEBASE OVERVIEW:
        - Total files: ${context.totalFiles}
        - Languages: ${context.languages.joinToString(", ")}
        - Top hotspots: ${context.hotspots.take(5).joinToString(", ") { File(it).name }}
        
        RECENT CHANGES:
        ${context.recentChanges.take(3).joinToString("\n") { "- ${it.file}: ${it.message}" }}
        
        DEVELOPER QUESTION: "$question"
        
        Respond with JSON:
        {
          "answer": "Clear, helpful answer (2-3 sentences)",
          "suggestedFiles": ["file1.kt", "file2.java"],
          "confidence": 0.0-1.0
        }
        
        Be concise and actionable. If you don't know, say so.
        """
        }

        private suspend fun callClaude(prompt: String): String {
                // FALLBACK FOR DEMO / FREE MODE
                if (apiKey.isBlank() || apiKey == "heuristic") {
                        delay(500) // Simulate network latency
                        // Extract file name from prompt to make it context-aware
                        val fileName =
                                prompt.lines()
                                        .find { it.trim().startsWith("FILE:") }
                                        ?.substringAfter(":")
                                        ?.trim()
                                        ?: "Unknown.kt"

                        return """
            {
              "purpose": "A core component of the system responsible for logic related to $fileName.",
              "complexity": 4,
              "keyComponents": ["Class definition", "Functions"],
              "refactoringTips": ["Consider splitting large functions", "Add KDoc documentation"],
              "securityConcerns": [],
              "businessImpact": "Maintains stability of the core workflow.",
              "readingTime": 5,
              "answer": "Based on the static analysis of $fileName, it appears to be a key logic file. I recommend checking its dependencies in the graph.",
              "suggestedFiles": ["$fileName"],
              "confidence": 0.8
            }
            """
                }

                val requestBody =
                        """
        {
          "model": "$model",
          "max_tokens": 1000,
          "messages": [
            {
              "role": "user",
              "content": "${ prompt.replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t") }"
            }
          ]
        }
        """.trimIndent()

                val request =
                        HttpRequest.newBuilder()
                                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                                .header("Content-Type", "application/json")
                                .header("x-api-key", apiKey)
                                .header("anthropic-version", "2023-06-01")
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                .build()

                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() != 200) {
                        throw Exception(
                                "Claude API error: ${response.statusCode()} ${response.body()}"
                        )
                }

                // Parse response
                val responseJson = Json.parseToJsonElement(response.body()).jsonObject
                val content = responseJson["content"]?.jsonArray?.firstOrNull()?.jsonObject
                return content?.get("text")
                        ?.toString()
                        ?.removeSurrounding("\"")
                        ?.replace("\\n", "\n")
                        ?: throw Exception("Invalid response format")
        }

        private fun parseInsight(response: String, filePath: String): AIInsight {
                // Extract JSON from response (Claude might add markdown)
                val jsonText =
                        response.substringAfter("{")
                                .substringBeforeLast("}")
                                .let { "{$it}" }
                                .replace(
                                        "\\\"",
                                        "\""
                                ) // Clean up escaped quotes if any double escaping happened

                return try {
                        json.decodeFromString<AIInsight>(jsonText).copy(file = filePath)
                } catch (e: Exception) {
                        // Fallback to basic insight
                        AIInsight(
                                file = filePath,
                                purpose = "Analysis unavailable (JSON error)",
                                complexity = 5,
                                keyComponents = emptyList(),
                                refactoringTips = emptyList(),
                                securityConcerns = emptyList(),
                                businessImpact = "Unknown",
                                readingTime = 10
                        )
                }
        }

        private fun parseConversation(response: String): AIConversationResponse {
                val jsonText = response.substringAfter("{").substringBeforeLast("}").let { "{$it}" }

                return try {
                        json.decodeFromString<AIConversationResponse>(jsonText)
                } catch (e: Exception) {
                        AIConversationResponse(
                                answer = response.take(200) + "...",
                                suggestedFiles = emptyList(),
                                confidence = 0.5
                        )
                }
        }
}

data class AnalysisContext(
        val totalFiles: Int,
        val dependencies: Int,
        val dependents: Int,
        val pageRank: Double,
        val gitChurn: Int
)

data class CodebaseContext(
        val totalFiles: Int,
        val languages: List<String>,
        val hotspots: List<String>,
        val recentChanges: List<GitChange>
)

data class GitChange(val file: String, val message: String, val author: String)
