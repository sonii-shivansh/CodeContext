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
    private const val VERICORE_GEMINI_API_KEY = "VERICORE_GEMINI_API_KEY"
    private const val VERICORE_GOOGLE_API_KEY = "VERICORE_GOOGLE_API_KEY"
    private const val LEGACY_GEMINI_API_KEY = "GEMINI_API_KEY"
    private const val LEGACY_GOOGLE_API_KEY = "GOOGLE_API_KEY"
    private const val AI_PROVIDER = "VERICORE_AI_PROVIDER"
    private const val AI_MODEL = "VERICORE_AI_MODEL"
    private const val LEGACY_AI_PROVIDER = "CODECONTEXT_AI_PROVIDER"
    private const val LEGACY_AI_MODEL = "CODECONTEXT_AI_MODEL"
    private const val LEGACY_GEMINI_MODEL = "gemini-2.5-flash"
    private const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"

    const val DEFAULT_CONFIG_FILE = ".vericore.json"
    const val LEGACY_CONFIG_FILE = ".codecontext.json"

    private val logger = KotlinLogging.logger {}

    fun load(configPath: String = DEFAULT_CONFIG_FILE): CodeContextConfig {
        val file = resolveConfigFile(File(configPath))
        return if (file.exists()) {
            try {
                Json { ignoreUnknownKeys = true }.decodeFromString<CodeContextConfig>(file.readText())
            } catch (e: Exception) {
                logger.warn(e) { "Failed to parse config at ${file.path}, using defaults" }
                System.err.println("⚠️ Failed to parse config, using defaults: ${e.message}")
                CodeContextConfig()
            }
        } else {
            logger.debug { "Config file not found at ${file.path}, using defaults" }
            CodeContextConfig()
        }
    }

    /** Load project configuration relative to the repository being analyzed, not the CLI process cwd. */
    fun loadForRepository(repoPath: String): CodeContextConfig =
        load(File(repoPath).canonicalFile.resolve(DEFAULT_CONFIG_FILE).path)

    /** Resolves environment -> project credential -> user credential -> defaults. */
    fun loadEffective(
        configPath: String = DEFAULT_CONFIG_FILE,
        environment: Map<String, String> = System.getenv()
    ): CodeContextConfig {
        val project = load(configPath)
        val user = UserConfigStore.load()?.ai
        val environmentKey = firstNonBlank(
            environment[VERICORE_GEMINI_API_KEY],
            environment[VERICORE_GOOGLE_API_KEY],
            environment[LEGACY_GEMINI_API_KEY],
            environment[LEGACY_GOOGLE_API_KEY]
        )
        val projectKey = project.ai.apiKey.trim().takeIf { it.isNotEmpty() }
        val userKey = user?.apiKey?.trim()?.takeIf { it.isNotEmpty() }

        val key = environmentKey ?: projectKey ?: userKey.orEmpty()
        val provider = firstNonBlank(
            environment[AI_PROVIDER],
            environment[LEGACY_AI_PROVIDER]
        ) ?: if (projectKey == null && user != null) user.provider else project.ai.provider
        val configuredModel = firstNonBlank(
            environment[AI_MODEL],
            environment[LEGACY_AI_MODEL]
        ) ?: if (projectKey == null && user != null) user.model else project.ai.model
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

    fun createDefault(path: String = DEFAULT_CONFIG_FILE) {
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

    private fun resolveConfigFile(requested: File): File {
        if (requested.exists() || requested.name != DEFAULT_CONFIG_FILE) return requested
        val legacy = File(requested.parentFile ?: File("."), LEGACY_CONFIG_FILE)
        if (legacy.exists()) {
            logger.warn { "Using legacy CodeContext configuration ${legacy.path}; migrate to ${requested.path}" }
            return legacy
        }
        return requested
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.asSequence().mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.firstOrNull()
}
