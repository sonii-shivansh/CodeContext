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
    val excludePaths: List<String> = listOf(
        ".git", ".idea", ".gradle", "build", "target", "node_modules", ".vscode", "out", "dist", ".next"
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
    val provider: String = "gemini",
    val apiKey: String = "",
    val model: String = "gemini-2.5-flash"
)

@Serializable
data class RateLimitConfig(
    val enabled: Boolean = true,
    val requestsPerMinute: Int = 60,
    val requestsPerHour: Int = 1000
)

object ConfigLoader {
    private const val GEMINI_API_KEY = "GEMINI_API_KEY"
    private const val GOOGLE_API_KEY = "GOOGLE_API_KEY"
    private const val AI_PROVIDER = "CODECONTEXT_AI_PROVIDER"
    private const val AI_MODEL = "CODECONTEXT_AI_MODEL"

    private val logger = KotlinLogging.logger {}

    /** Loads only repository-local configuration. Preserved for existing callers. */
    fun load(configPath: String = ".codecontext.json"): CodeContextConfig {
        val file = File(configPath)
        return if (file.exists()) {
            try {
                Json { ignoreUnknownKeys = true }.decodeFromString<CodeContextConfig>(file.readText())
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

    /**
     * Resolves the developer-friendly configuration hierarchy:
     * environment -> project credential -> user credential -> defaults.
     * Secrets are never written into the repository by this method.
     */
    fun loadEffective(configPath: String = ".codecontext.json"): CodeContextConfig {
        val project = load(configPath)
        val user = UserConfigStore.load()?.ai
        val environmentKey = System.getenv(GEMINI_API_KEY)?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: System.getenv(GOOGLE_API_KEY)?.trim()?.takeIf { it.isNotEmpty() }
        val projectKey = project.ai.apiKey.trim().takeIf { it.isNotEmpty() }
        val userKey = user?.apiKey?.trim()?.takeIf { it.isNotEmpty() }

        val key = environmentKey ?: projectKey ?: userKey.orEmpty()
        val provider = System.getenv(AI_PROVIDER)?.trim()?.takeIf { it.isNotEmpty() }
            ?: if (projectKey == null && user != null) user.provider else project.ai.provider
        val model = System.getenv(AI_MODEL)?.trim()?.takeIf { it.isNotEmpty() }
            ?: if (projectKey == null && user != null) user.model else project.ai.model

        return project.copy(
            ai = project.ai.copy(
                enabled = project.ai.enabled || key.isNotBlank(),
                provider = provider,
                apiKey = key,
                model = model
            )
        )
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
