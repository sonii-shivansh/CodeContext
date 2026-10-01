package com.codecontext.cli

import com.codecontext.core.workflow.EngineeringPreparation
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Creates a reusable evidence snapshot and engineering plan before implementation. */
class PrepareCommand : CliktCommand(name = "prepare", help = "Prepare an evidence-backed change plan before coding") {
    private val changeSummary by argument("change-summary", help = "Short description of the proposed change")
    private val path by option("--path", help = "Repository path").default(".")
    private val output by option("--output", help = "Preparation artifact path").default("output/engineering-context.json")
    private val planOutput by option("--plan-output", help = "Engineering plan artifact path").default("output/engineering-plan.json")

    override fun run() {
        val result = runBlocking { EngineeringPreparation.prepare(path, changeSummary) }
        val json = Json { prettyPrint = true; encodeDefaults = true }
        val artifact = File(output).apply { parentFile?.mkdirs() }
        artifact.writeText(json.encodeToString(result))
        val planFile = File(planOutput).apply { parentFile?.mkdirs() }
        planFile.writeText(json.encodeToString(result.plan))
        echo("Engineering context: ${artifact.path}")
        echo("Engineering plan: ${planFile.path}")
        echo("Risk: ${result.plan.riskLevel}")
        echo("Affected components: ${result.plan.affectedComponents.size}")
    }
}
