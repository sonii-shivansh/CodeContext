package com.codecontext.core.ai

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Small, deterministic Gemini client used by the interactive `ask` command. */
class GeminiQuestionService(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL
) {
    companion object {
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        private val FALLBACK_MODELS = listOf("gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.6-flash")
        private val RETRYABLE_STATUS_CODES = setOf(429, 500, 502, 503, 504)
    }

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build()

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun ask(question: String, context: CodebaseContext): AIConversationResponse {
        val prompt = buildPrompt(question, context)
        val models = listOf(model) + FALLBACK_MODELS.filter { it != model }
        var lastFailure = "unknown error"

        for (candidateModel in models) {
            val requestBody = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", buildJsonObject {
                    put("maxOutputTokens", 2048)
                })
            }.toString()

            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/$candidateModel:generateContent"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            repeat(3) { attempt ->
                val response = runCatching { client.send(request, HttpResponse.BodyHandlers.ofString()) }
                    .getOrElse {
                        lastFailure = it.message ?: it::class.simpleName.orEmpty()
                        if (attempt < 2) Thread.sleep(1_000L * (1L shl attempt))
                        return@repeat
                    }
                val body = response.body()
                if (response.statusCode() in 200..299) return parseGeminiResponse(body)

                val detail = body.replace(Regex("\\s+"), " ").take(300)
                lastFailure = "$candidateModel HTTP ${response.statusCode()}: $detail"
                if (response.statusCode() !in RETRYABLE_STATUS_CODES) {
                    throw IllegalStateException("Gemini request failed with $lastFailure")
                }
                if (attempt < 2) Thread.sleep(1_000L * (1L shl attempt))
            }
        }

        throw IllegalStateException("Gemini request failed after model fallbacks: $lastFailure")
    }

    private fun parseGeminiResponse(body: String): AIConversationResponse {
        val responseJson = json.parseToJsonElement(body).jsonObject
        val text = responseJson["candidates"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray
            ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }
            ?.joinToString("\n")
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini returned no text content")

        return parseResponse(text)
    }

    private fun buildPrompt(question: String, context: CodebaseContext): String = """
        You're an expert guide for this codebase.

        CODEBASE OVERVIEW:
        - Total files: ${context.totalFiles}
        - Languages: ${context.languages.joinToString(", ")}
        - Top hotspots: ${context.hotspots.take(5).joinToString(", ") { java.io.File(it).name }}

        RECENT CHANGES:
        ${context.recentChanges.take(3).joinToString("\n") { "- ${it.file}: ${it.message}" }}

        DEVELOPER QUESTION: "${sanitize(question)}"

        Respond with JSON:
        {
          "answer": "Clear, helpful answer (2-3 sentences)",
          "suggestedFiles": ["file1.kt", "file2.java"],
          "confidence": 0.0-1.0
        }

        Be concise and actionable. If you don't know, say so.
    """.trimIndent()

    private fun parseResponse(raw: String): AIConversationResponse {
        val payload = extractJson(raw)
        return runCatching {
            val parsed = json.decodeFromString<AIConversationResponse>(payload)
            parsed.copy(confidence = parsed.confidence.coerceIn(0.0, 1.0))
        }.getOrElse {
            AIConversationResponse(raw.trim(), emptyList(), 0.0)
        }
    }

    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        val fenced = Regex("```(?:json)?\\s*(\\{.*?\\})\\s*```", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(trimmed)?.groupValues?.getOrNull(1)
        if (fenced != null) return fenced.trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    }

    private fun sanitize(input: String): String {
        var output = input
        val patterns = listOf(
            Regex("(?i)(api[_-]?key|secret|token|password|passwd|authorization)[\\s:=]+[A-Za-z0-9._~+/=-]{8,}"),
            Regex("(?i)(ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]+|AIza[0-9A-Za-z_-]{10,}|sk-[A-Za-z0-9]{10,}|AKIA[0-9A-Z]{16})")
        )
        for (pattern in patterns) output = pattern.replace(output, "***REDACTED***")
        return output
    }
}
