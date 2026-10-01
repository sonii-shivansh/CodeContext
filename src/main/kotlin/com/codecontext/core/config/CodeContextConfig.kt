package com.codecontext.core.config

import com.codecontext.core.exceptions.ConfigurationException
import com.codecontext.core.intelligence.ArchitectureRuleConfig
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

@Serializable
data class CodeContextConfig(
        val excludePaths: List<String> =
                listOf(
                        ".git",
                        ".idea",
                        ".gradle",
                        "build",
                        "target",
                        "node_modules",
                        ".vscode",
                        "out",
                        "dist",
                        ".next"
                ),
        val maxFilesAnalyze: Int = 5000,
        val gitCommitLimit: Int = 1000,
        val enableCache: Boolean = true,
        val enableParallel: Boolean = true,
        val hotspotCount: Int = 15,
        val learningPathLength: Int = 20,
        val ai: AIConfig = AIConfig(),
        val rateLimit: RateLimitConfig = RateLimitConfig(),
        val architecture: ArchitectureRuleConfig = ArchitectureRuleConfig()
)

@Serializable
data class AIConfig(
        val enabled: Boolean = false,
        val provider: String = "anthropic", // "anthropic" or "openai"
        val apiKey: String = "",
        val model: String = "claude-sonnet-4-20250514"
)

@Serializable
data class RateLimitConfig(
        val enabled: Boolean = true,
        val requestsPerMinute: Int = 60,
        val requestsPerHour: Int = 1000
)

object ConfigLoader {
    private val logger = KotlinLogging.logger {}

    fun load(configPath: String = ".codecontext.json"): CodeContextConfig {
        val file = File(configPath)

        return if (file.exists()) {
            try {
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString<CodeContextConfig>(file.readText())
            } catch (e: Exception) {
                logger.warn(e) { "Failed to parse config at $configPath, using defaults" }
                System.err.println("⚠️ Failed to parse config, using defaults: ${e.message}")
                CodeContextConfig()
            }
        } else {
            logger.debug { "Config file not found at $configPath, using defaults" }
            CodeContextConfig()
        }
    }

    fun createDefault(path: String = ".codecontext.json") {
        try {
            val config = CodeContextConfig()
            val json = Json {
                prettyPrint = true
                encodeDefaults = true
            }
            File(path).writeText(json.encodeToString(config))
            logger.info { "Created default config at $path" }
            println("✅ Created default config at $path")
        } catch (e: Exception) {
            throw ConfigurationException("Failed to create default config at $path", e)
        }
    }
}
