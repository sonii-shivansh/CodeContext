package com.vericore.core.config

import com.vericore.core.exceptions.ConfigurationException
import com.vericore.core.intelligence.ArchitectureRuleConfig
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

@Serializable
data class VericoreConfig(
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

@Deprecated("CodeContextConfig is a temporary compatibility alias. Use VericoreConfig.", ReplaceWith("VericoreConfig"))
typealias CodeContextConfig = VericoreConfig

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
    private const val LEGACY_AI_PROVIDER = "CODECONTEXT_AI_PROVIDER"
    private const val LEGACY_AI_MODEL = "CODECONTEXT_AI_MODEL"
    private const val LEGACY_CONFIG_FILE = ".codecontext.json"
    private const val LEGACY_GEMINI_MODEL = "gemini-2.5-flash"
    private const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"

    private val logger = KotlinLogging.logger {}

    fun load(configPath: String = ".vericore.json"): VericoreConfig {
        val file = File(configPath)
        val legacy = if (!file.exists() && configPath.endsWith(".vericore.json")) File(configPath.removeSuffix(".vericore.json") + LEGACY_CONFIG_FILE) else null
        val selected = when {
            file.exists() -> file
            legacy?.exists() == true -> {
                System.err.println("⚠️ Deprecated CodeContext configuration detected at ${legacy.path}; migrate it to ${File(configPath).path}.")
                legacy
            }
            else -> null
        }
        return if (selected != null) {
            try {
                Json { ignoreUnknownKeys = true }.decodeFromString<VericoreConfig>(selected.readText())
            } catch (e: Exception) {
                logger.warn(e) { "Failed to parse config at ${selected.path}, using defaults" }
                System.err.println("⚠️ Failed to parse config, using defaults: ${e.message}")
                VericoreConfig()
            }
        } else {
            logger.debug { "Config file not found at $configPath, using defaults" }
            VericoreConfig()
        }
    }

    fun loadForRepository(repoPath: String): VericoreConfig =
        load(File(repoPath).canonicalFile.resolve(".vericore.json").path)

    fun loadEffective(configPath: String = ".vericore.json"): VericoreConfig {
        val project = load(configPath)
        val user = UserConfigStore.load()?.ai
        val environmentKey = System.getenv(GEMINI_API_KEY)?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: System.getenv(GOOGLE_API_KEY)?.trim()?.takeIf { it.isNotEmpty() }
        val projectKey = project.ai.apiKey.trim().takeIf { it.isNotEmpty() }
        val userKey = user?.apiKey?.trim()?.takeIf { it.isNotEmpty() }.orEmpty()

        val key = environmentKey ?: projectKey ?: userKey
        val canonicalProvider = System.getenv(AI_PROVIDER)?.trim()?.takeIf { it.isNotEmpty() }
        val canonicalModel = System.getenv(AI_MODEL)?.trim()?.takeIf { it.isNotEmpty() }
        val legacyProvider = System.getenv(LEGACY_AI_PROVIDER)?.trim()?.takeIf { it.isNotEmpty() }
        val legacyModel = System.getenv(LEGACY_AI_MODEL)?.trim()?.takeIf { it.isNotEmpty() }
        if (canonicalProvider == null && legacyProvider != null) System.err.println("⚠️ Deprecated CODECONTEXT_AI_PROVIDER is in use; replace it with VERICORE_AI_PROVIDER.")
        if (canonicalModel == null && legacyModel != null) System.err.println("⚠️ Deprecated CODECONTEXT_AI_MODEL is in use; replace it with VERICORE_AI_MODEL.")

        val provider = canonicalProvider ?: legacyProvider ?: if (projectKey == null && user != null) user.provider else project.ai.provider
        val configuredModel = canonicalModel ?: legacyModel ?: if (projectKey == null && user != null) user.model else project.ai.model
        val model = if (configuredModel == LEGACY_GEMINI_MODEL) DEFAULT_GEMINI_MODEL else configuredModel

        return project.copy(ai = project.ai.copy(enabled = project.ai.enabled || key.isNotBlank(), provider = provider, apiKey = key, model = model))
    }

    fun createDefault(path: String = ".vericore.json") {
        try {
            val config = VericoreConfig()
            val json = Json { prettyPrint = true; encodeDefaults = true }
            File(path).writeText(json.encodeToString(config))
            logger.info { "Created default config at $path" }
            println("✅ Created default config at $path")
        } catch (e: Exception) {
            throw ConfigurationException("Failed to create default config at $path", e)
        }
    }
}
