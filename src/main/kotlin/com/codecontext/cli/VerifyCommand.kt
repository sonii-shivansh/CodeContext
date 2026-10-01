package com.codecontext.cli

import com.codecontext.core.planner.EngineeringPlan
import com.codecontext.core.workflow.EngineeringVerification
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/** Verifies the working-tree change against an evidence-backed engineering plan. */
class VerifyCommand : CliktCommand(name = "verify", help = "Verify the current change against an engineering plan") {
    private val path by option("--path", help = "Repository path").default(".")
    private val planFile by option("--plan", help = "Engineering plan JSON artifact").default("output/engineering-plan.json")
    private val output by option("--output", help = "Optional verification artifact path")

    override fun run() {
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
        val plan = json.decodeFromString<EngineeringPlan>(File(planFile).readText())
        val result = runBlocking { EngineeringVerification.verify(path, plan) }
        val encoded = json.encodeToString(result)
        if (output != null) {
            val file = File(output!!).apply { parentFile?.mkdirs() }
            file.writeText(encoded)
            echo("Verification report: ${file.path}")
        } else {
            echo(encoded)
        }
        echo("Status: ${result.status}")
        if (result.status == com.codecontext.core.workflow.SafetyStatus.FAIL) {
            throw IllegalStateException("Change verification failed: unexpected files are outside the engineering plan")
        }
    }
}
