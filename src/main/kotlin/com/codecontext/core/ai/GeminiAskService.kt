package com.codecontext.core.ai

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Small, typed Gemini client used by the interactive repository Q&A path. */
class GeminiAskService(
    private val apiKey: String,
    private val model: String = "gemini-3.8-flash"
) {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun ask(prompt: String): AIConversationResponse {
        require(apiKey.isNotBlank()) { "Gemini API key is not configured" }
        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", prompt) })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("maxOutputTokens", 2048)
            }
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Gemini provider request failed with status ${response.statusCode()}")
        }

        val body = json.parseToJsonElement(response.body()).jsonObject
        val text = body["candidates"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            ?: throw IllegalStateException("Gemini response did not contain generated text")

        return parseConversation(text)
    }

    private fun parseConversation(response: String): AIConversationResponse {
        val payload = response.trim().let { raw ->
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start >= 0 && end > start) raw.substring(start, end + 1) else raw
        }
        return runCatching {
            val parsed = json.decodeFromString<AIConversationResponse>(payload)
            parsed.copy(confidence = parsed.confidence.coerceIn(0.0, 1.0))
        }.getOrElse {
            AIConversationResponse(
                answer = response.trim().take(2000),
                suggestedFiles = emptyList(),
                confidence = 0.5
            )
        }
    }
}
