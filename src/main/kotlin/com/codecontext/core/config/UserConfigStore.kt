package com.codecontext.core.config

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class StoredAIConfig(
    val provider: String = "gemini",
    val apiKey: String = "",
    val model: String = "gemini-2.5-flash"
)

@Serializable
data class UserConfig(
    val ai: StoredAIConfig = StoredAIConfig()
)

/** Stores credentials outside the repository so secrets never need to live in .codecontext.json. */
object UserConfigStore {
    private const val CONFIG_HOME_ENV = "CODECONTEXT_CONFIG_HOME"

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun configFile(): File = File(configDirectory(), "config.json")

    fun load(): UserConfig? {
        val file = configFile()
        if (!file.isFile) return null
        return runCatching { json.decodeFromString<UserConfig>(file.readText()) }.getOrNull()
    }

    fun saveAi(provider: String, apiKey: String, model: String) {
        require(apiKey.isNotBlank()) { "API key must not be blank" }
        val file = configFile()
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(UserConfig(StoredAIConfig(provider, apiKey, model))))
        restrictPermissions(file)
    }

    fun delete(): Boolean = configFile().delete()

    private fun configDirectory(): File {
        val explicit = System.getenv(CONFIG_HOME_ENV)?.trim().orEmpty()
        if (explicit.isNotEmpty()) return File(explicit)

        val os = System.getProperty("os.name", "").lowercase()
        return when {
            os.contains("win") -> File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "CodeContext")
            os.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support/CodeContext")
            else -> {
                val xdg = System.getenv("XDG_CONFIG_HOME")?.trim().orEmpty()
                File(if (xdg.isNotEmpty()) xdg else File(System.getProperty("user.home"), ".config").absolutePath, "codecontext")
            }
        }
    }

    private fun restrictPermissions(file: File) {
        // POSIX permissions are available on Linux/macOS. Windows relies on the user's profile ACL.
        runCatching {
            val permissions = setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            )
            Files.setPosixFilePermissions(file.toPath(), permissions)
        }
    }
}
