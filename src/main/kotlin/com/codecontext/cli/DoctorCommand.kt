package com.codecontext.cli

import com.codecontext.core.Version
import com.codecontext.core.ai.AISetup
import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.config.UserConfigStore
import com.github.ajalt.clikt.core.CliktCommand
import java.io.File

class DoctorCommand : CliktCommand(
    name = "doctor",
    help = "Check the local CodeContext installation and configuration"
) {
    override fun run() {
        var failures = 0

        fun check(label: String, ok: Boolean, detail: String) {
            if (ok) echo("✓ $label — $detail")
            else {
                failures++
                echo("❌ $label — $detail")
            }
        }

        echo("CodeContext doctor")
        echo("Version: ${Version.current}")
        echo("")

        val javaMajor = Runtime.version().feature()
        check("Java runtime", javaMajor >= 21, "Java $javaMajor${if (javaMajor >= 21) " (supported)" else " (requires 21+)"}")

        val root = File(".").absoluteFile
        check("Repository", File(root, ".git").exists(), root.absolutePath)

        val projectConfig = File(".codecontext.json")
        val userConfig = UserConfigStore.load()
        val environmentKey = System.getenv("GEMINI_API_KEY")?.trim().orEmpty()
        val effective = ConfigLoader.loadEffective()
        val keySource = when {
            environmentKey.isNotBlank() -> "GEMINI_API_KEY environment variable"
            projectConfig.isFile && effective.ai.apiKey.isNotBlank() -> "project .codecontext.json"
            userConfig?.ai?.apiKey?.isNotBlank() == true -> "user configuration"
            else -> "not configured"
        }
        check("AI provider", effective.ai.provider.isNotBlank(), effective.ai.provider)
        check("AI credentials", effective.ai.apiKey.isNotBlank(), keySource)

        if (effective.ai.provider.equals("gemini", ignoreCase = true) && effective.ai.apiKey.isNotBlank()) {
            echo("   Validating Gemini credentials...")
            when (val result = AISetup.validateGemini(effective.ai.apiKey, effective.ai.model)) {
                AISetup.Success -> echo("✓ Gemini API", "reachable and credentials accepted")
                is AISetup.Failure -> {
                    failures++
                    echo("❌ Gemini API — ${result.message}")
                }
            }
        }

        echo("")
        if (failures == 0) {
            echo("✓ CodeContext is ready to use.")
        } else {
            echo("❌ $failures check(s) need attention.")
            echo("   Run 'codecontext setup' for interactive AI setup.")
        }
    }
}
