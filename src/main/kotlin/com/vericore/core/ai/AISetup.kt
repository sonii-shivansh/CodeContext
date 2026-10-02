package com.vericore.core.ai

import com.vericore.core.Version
import com.vericore.core.config.UserConfigStore
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

sealed class AISetupResult {
    data object Success : AISetupResult()
    data class Failure(val message: String) : AISetupResult()
}

object AISetup {
    const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun configureGemini(apiKey: String, model: String = DEFAULT_GEMINI_MODEL, persist: Boolean = true): AISetupResult {
        val key = apiKey.trim()
        if (key.isBlank()) return AISetupResult.Failure("API key cannot be empty.")

        val validation = validateGemini(key, model)
        if (validation is AISetupResult.Failure) return validation

        if (persist) UserConfigStore.saveAi("gemini", key, model)
        return AISetupResult.Success
    }

    fun validateGemini(apiKey: String, model: String = DEFAULT_GEMINI_MODEL): AISetupResult {
        val request = runCatching {
            HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/$model"))
                .timeout(Duration.ofSeconds(15))
                .header("x-goog-api-key", apiKey)
                .header("x-goog-api-client", "codecontext/${Version.current}")
                .GET()
                .build()
        }.getOrElse { return AISetupResult.Failure("Could not prepare Gemini validation request: ${it.message}") }

        val response = runCatching {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }.getOrElse {
            return AISetupResult.Failure("Could not reach Gemini API: ${it.message ?: it::class.simpleName}")
        }

        if (response.statusCode() in 200..299) return AISetupResult.Success
        val detail = response.body().replace(Regex("\\s+"), " ").take(240)
        return AISetupResult.Failure("Gemini rejected the API key (HTTP ${response.statusCode()}). $detail")
    }
}
