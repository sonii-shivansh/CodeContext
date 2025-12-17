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
        val complexity: Int,
        val keyComponents: List<String>,
        val refactoringTips: List<String>,
        val securityConcerns: List<String>,
        val businessImpact: String,
        val readingTime: Int
)

@Serializable
data class AIConversationResponse(
        val answer: String,
        val suggestedFiles: List<String>,
        val confidence: Double
)

@Serializable
data class PRReview(
        val summary: String,
        val impactAnalysis: String,
        val securityRisks: List<String>,
        val breakingChanges: List<String>,
        val suggestions: List<CodeSuggestion>,
        val hotspotImpact: String?
)

@Serializable
data class CodeSuggestion(
        val file: String,
        val line: Int,
        val suggestion: String,
        val severity: String
)

class AICodeAnalyzer(
        private val apiKey: String,
        private val model: String = "gemini-2.5-flash",
        private val provider: String = "gemini"
) {
        private val client = HttpClient.newHttpClient()
        private val json = Json { ignoreUnknownKeys = true }

        private val isEnabled: Boolean =
                apiKey.isNotBlank() && apiKey != "heuristic" && !apiKey.startsWith("demo")

        fun isConfigured(): Boolean = isEnabled

        /** Analyze a single file and generate comprehensive insights */
        suspend fun analyzeFile(file: ParsedFile, context: AnalysisContext): AIInsight =
                withContext(Dispatchers.IO) {
                        if (!isEnabled) {
                                throw IllegalStateException(
                                        "AI analysis is not configured. Please set a valid API key in .codecontext.json"
                                )
                        }

                        val prompt = buildFileAnalysisPrompt(file, context)
                        val response = callAI(prompt)

                        return@withContext parseInsight(response, file.file.absolutePath)
                }

        /** Batch analyze multiple files efficiently */
        suspend fun batchAnalyze(
                files: List<ParsedFile>,
                graph: RobustDependencyGraph,
                limit: Int = 50
        ): Map<String, AIInsight> = coroutineScope {
                val prioritized =
                        files
                                .sortedByDescending {
                                        graph.pageRankScores[it.file.absolutePath] ?: 0.0
                                }
                                .take(limit)

                println("🤖 AI analyzing top $limit files...")

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
                        val response = callAI(prompt)

                        return@withContext parseConversation(response)
                }

        /** Review a Pull Request / Diff */
        suspend fun reviewPullRequest(
                files: List<String>,
                diff: String,
                graph: RobustDependencyGraph
        ): PRReview =
                withContext(Dispatchers.IO) {
                        if (!isEnabled) {
                                throw IllegalStateException(
                                        "AI analysis is not configured. Please set a valid API key."
                                )
                        }

                        val hotspots = graph.getTopHotspots(20).map { it.first }
                        val affectedHotspots =
                                files.filter { file ->
                                        hotspots.any { file.endsWith(it) || it.endsWith(file) }
                                }

                        val prompt = buildPRReviewPrompt(files, diff, affectedHotspots)
                        val response = callAI(prompt)

                        return@withContext parsePRReview(response)
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

                        callAI(prompt).trim()
                }

        /** Suggest learning order based on complexity and dependencies */
        suspend fun generateLearningStrategy(
                files: List<ParsedFile>,
                developerLevel: String
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

                        callAI(prompt)
                }

        private fun buildFileAnalysisPrompt(file: ParsedFile, context: AnalysisContext): String {
                val fileContent =
                        try {
                                file.file.readText().take(3000)
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

        private fun buildPRReviewPrompt(
                files: List<String>,
                diff: String,
                affectedHotspots: List<String>
        ): String {
                return """
        Review the following code changes (Pull Request) for a codebase.
        
        CHANGED FILES:
        ${files.joinToString("\n") { "- $it" }}
        
        CRITICAL HOTSPOTS AFFECTED:
        ${if (affectedHotspots.isEmpty()) "None" else affectedHotspots.joinToString(", ")}
        
        DIFF:
        ```diff
        ${diff.take(5000)} ${if (diff.length > 5000) "...(truncated)" else ""}
        ```
        
        Analyze for:
        1. Correctness and Logic Errors
        2. Security Vulnerabilities
        3. Potential Breaking Changes
        4. Performance Implications
        5. Impact on identified Hotspots
        
        Respond ONLY with JSON:
        {
          "summary": "Brief executive summary",
          "impactAnalysis": "Assessment of risk",
          "securityRisks": ["risk1"] or [],
          "breakingChanges": ["change1"] or [],
          "hotspotImpact": "Analysis" or null,
          "suggestions": [
            {
              "file": "path/to/file",
              "line": 0,
              "suggestion": "Specific advice",
              "severity": "critical" | "major" | "minor"
            }
          ]
        }
        """
        }

        /** Universal AI caller - supports both Gemini and Claude */
        private suspend fun callAI(prompt: String): String {
                return when (provider.lowercase()) {
                        "gemini" -> callGemini(prompt)
                        "anthropic", "claude" -> callClaude(prompt)
                        else -> throw IllegalArgumentException("Unsupported AI provider: $provider")
                }
        }

        /** Call Google Gemini API */
        private suspend fun callGemini(prompt: String): String {
                val requestBody =
                        """
                {
                  "contents": [{
                    "parts": [{
                      "text": ${Json.encodeToString(prompt)}
                    }]
                  }],
                  "generationConfig": {
                    "temperature": 0.7,
                    "topK": 40,
                    "topP": 0.95,
                    "maxOutputTokens": 2048
                  }
                }
                """.trimIndent()

                val url =
                        "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

                val request =
                        HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                .build()

                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() != 200) {
                        throw Exception(
                                "Gemini API error: ${response.statusCode()} ${response.body()}"
                        )
                }

                // Parse Gemini response
                val responseJson = Json.parseToJsonElement(response.body()).jsonObject
                val candidates = responseJson["candidates"]?.jsonArray
                val firstCandidate = candidates?.firstOrNull()?.jsonObject
                val content = firstCandidate?.get("content")?.jsonObject
                val parts = content?.get("parts")?.jsonArray
                val text = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content

                return text ?: throw Exception("Invalid Gemini response format: ${response.body()}")
        }

        /** Call Anthropic Claude API (original implementation) */
        private suspend fun callClaude(prompt: String): String {
                val requestBody =
                        """
                {
                  "model": "$model",
                  "max_tokens": 1000,
                  "messages": [
                    {
                      "role": "user",
                      "content": "${prompt.replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t")}"
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

                val responseJson = Json.parseToJsonElement(response.body()).jsonObject
                val content = responseJson["content"]?.jsonArray?.firstOrNull()?.jsonObject
                return content?.get("text")
                        ?.toString()
                        ?.removeSurrounding("\"")
                        ?.replace("\\n", "\n")
                        ?: throw Exception("Invalid response format")
        }

        private fun parseInsight(response: String, filePath: String): AIInsight {
                val jsonText =
                        response.substringAfter("{")
                                .substringBeforeLast("}")
                                .let { "{$it}" }
                                .replace("\\\"", "\"")

                return try {
                        json.decodeFromString<AIInsight>(jsonText).copy(file = filePath)
                } catch (e: Exception) {
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

        private fun parsePRReview(response: String): PRReview {
                val jsonText = response.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
                return try {
                        json.decodeFromString<PRReview>(jsonText)
                } catch (e: Exception) {
                        PRReview(
                                summary = "Failed to parse AI response.",
                                impactAnalysis = "Unknown",
                                securityRisks = emptyList(),
                                breakingChanges = emptyList(),
                                suggestions = emptyList(),
                                hotspotImpact = null
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
