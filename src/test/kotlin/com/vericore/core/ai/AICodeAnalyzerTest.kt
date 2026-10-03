package com.vericore.core.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class AICodeAnalyzerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `gemini default model is the canonical Vericore model`() {
        assertEquals("gemini-3.8-flash", AICodeAnalyzer.DEFAULT_MODEL)
        assertEquals("gemini-3.8-flash", AICodeAnalyzer("test-key").configuredModel)
    }

    @Test
    fun `gemini request uses the 3 x contract without legacy sampling parameters`() {
        val request = json.parseToJsonElement(
            AICodeAnalyzer("test-key").buildGeminiRequestBody("hello")
        ).jsonObject

        val generationConfig = request["generationConfig"]?.jsonObject
        assertNotNull(generationConfig)
        assertEquals(2048, generationConfig["maxOutputTokens"]?.toString()?.toInt())
        assertFalse(generationConfig.containsKey("temperature"))
        assertFalse(generationConfig.containsKey("topK"))
        assertFalse(generationConfig.containsKey("topP"))
    }
}
