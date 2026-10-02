package com.vericore.cli

import com.vericore.core.ai.AISetup
import com.vericore.core.ai.AISetupResult
import com.vericore.core.config.UserConfigStore

object AISetupPrompter {
    fun ensureConfigured(model: String = "gemini-2.5-flash"): Boolean {
        val environmentKey = System.getenv("GEMINI_API_KEY")?.trim().orEmpty()
            .ifBlank { System.getenv("GOOGLE_API_KEY")?.trim().orEmpty() }
        if (environmentKey.isNotBlank()) {
            return when (AISetup.validateGemini(environmentKey, model)) {
                AISetupResult.Success -> true
                is AISetupResult.Failure -> false
            }
        }

        echo("\nCodeContext AI setup required.")
        echo("Gemini is the default AI provider.")
        echo("Add your Gemini API key now? [Y/n]: ", trailingNewline = false)
        val answer = readLine()?.trim().orEmpty().lowercase()
        if (answer.isNotEmpty() && answer !in setOf("y", "yes")) {
            echo("AI setup skipped.")
            return false
        }

        val key = readSecret("Gemini API key")
        if (key.isBlank()) {
            echo("❌ No API key supplied. AI remains disabled.")
            return false
        }

        echo("🔐 Validating Gemini credentials...")
        return when (val result = AISetup.configureGemini(key, model)) {
            AISetupResult.Success -> {
                echo("✓ Gemini API key validated and saved securely.")
                echo("  Config: ${UserConfigStore.configFile().absolutePath}")
                true
            }
            is AISetupResult.Failure -> {
                echo("❌ ${result.message}")
                echo("   Nothing was saved.")
                false
            }
        }
    }

    private fun readSecret(label: String): String {
        val console = System.console()
        if (console != null) return console.readPassword("%s: ", label).concatToString().trim()

        echo("⚠️ Secure terminal input is unavailable in this environment.")
        echo("   The key will be visible while typing.")
        echo("$label:", trailingNewline = false)
        return readLine()?.trim().orEmpty()
    }

    private fun echo(message: String, trailingNewline: Boolean = true) {
        if (trailingNewline) println(message) else print(message)
    }
}
