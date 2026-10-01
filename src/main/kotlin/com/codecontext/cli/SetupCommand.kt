package com.codecontext.cli

import com.codecontext.core.ai.AISetup
import com.codecontext.core.ai.AISetupResult
import com.codecontext.core.config.UserConfigStore
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class SetupCommand : CliktCommand(
    name = "setup",
    help = "Set up CodeContext AI credentials securely on this machine"
) {
    private val provider by option("--provider", help = "AI provider (currently: gemini)").default("gemini")
    private val model by option("--model", help = "Gemini model to use").default("gemini-3.8-flash")
    private val force by option("--force", help = "Replace an existing saved credential").flag()

    override fun run() {
        if (!provider.equals("gemini", ignoreCase = true)) {
            echo("❌ Unsupported provider: $provider")
            echo("   Supported provider: gemini")
            return
        }

        val existing = UserConfigStore.load()?.ai
        val environmentKey = System.getenv("GEMINI_API_KEY")?.trim().orEmpty()

        if (!force && existing?.apiKey?.isNotBlank() == true) {
            echo("✓ Gemini is already configured for this user.")
            echo("  Config: ${UserConfigStore.configFile().absolutePath}")
            echo("  Run 'codecontext doctor' to verify the setup.")
            return
        }

        val apiKey = environmentKey.ifBlank { readSecret("Gemini API key") }
        if (apiKey.isBlank()) {
            echo("❌ No API key supplied. Nothing was changed.")
            return
        }

        echo("🔐 Validating Gemini credentials...")
        when (val result = AISetup.configureGemini(apiKey, model)) {
            AISetupResult.Success -> {
                echo("✓ Gemini API key validated")
                echo("✓ Saved securely for this user")
                echo("  Config: ${UserConfigStore.configFile().absolutePath}")
                echo("\nYou can now run:")
                echo("  codecontext ask \"What is the architecture of this repository?\"")
            }
            is AISetupResult.Failure -> {
                echo("❌ ${result.message}")
                echo("   Nothing was saved.")
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
}
