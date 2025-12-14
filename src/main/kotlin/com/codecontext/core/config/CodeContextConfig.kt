package com.codecontext.core.config

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        val ai: AIConfig = AIConfig()
)

@Serializable
data class AIConfig(
        val enabled: Boolean = false,
        val provider: String = "anthropic", // "anthropic" or "openai"
        val apiKey: String = "",
        val model: String = "claude-sonnet-4-20250514"
)

object ConfigLoader {
    fun load(configPath: String = ".codecontext.json"): CodeContextConfig {
        val file = File(configPath)

        return if (file.exists()) {
            try {
                // Ignore unknown keys for forward compatibility
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString<CodeContextConfig>(file.readText())
            } catch (e: Exception) {
                println("⚠️ Failed to parse config, using defaults: ${e.message}")
                CodeContextConfig()
            }
        } else {
            CodeContextConfig()
        }
    }

    fun createDefault(path: String = ".codecontext.json") {
        val config = CodeContextConfig()
        val json = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        File(path).writeText(json.encodeToString(config))
        println("✅ Created default config at $path")
    }
}
