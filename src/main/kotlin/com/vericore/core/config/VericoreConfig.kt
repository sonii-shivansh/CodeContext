package com.vericore.core.config

import com.vericore.core.exceptions.ConfigurationException
import com.vericore.core.intelligence.ArchitectureRuleConfig
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
    val model: String = "gemini-3.8-flash"
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
    private const val AI_PROVIDER = "VERICORE_AI_PROVIDER"
    private const val AI_MODEL = "VERICORE_AI_MODEL"
    private const val LEGACY_GEMINI_MODEL = "gemini-2.5-flash"
    private const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"

    private val logger = KotlinLogging.logger {}

    fun load(configPath: String = ".vericore.json"): CodeContextConfig {
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

    /** Load project configuration relative to the repository being analyzed, not the CLI process cwd. */
    fun loadForRepository(repoPath: String): CodeContextConfig =
        load(File(repoPath).canonicalFile.resolve(".vericore.json").path)

    /** Resolves environment -> project credential -> user credential -> defaults. */
    fun loadEffective(configPath: String = ".vericore.json"): CodeContextConfig {
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
        val configuredModel = System.getenv(AI_MODEL)?.trim()?.takeIf { it.isNotEmpty() }
            ?: if (projectKey == null && user != null) user.model else project.ai.model
        val model = if (configuredModel == LEGACY_GEMINI_MODEL) DEFAULT_GEMINI_MODEL else configuredModel

        return project.copy(
            ai = project.ai.copy(
                enabled = project.ai.enabled || key.isNotBlank(),
                provider = provider,
                apiKey = key,
                model = model
            )
        )
    }

    fun createDefault(path: String = ".vericore.json") {
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
