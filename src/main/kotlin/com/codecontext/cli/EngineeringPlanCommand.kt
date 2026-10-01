package com.codecontext.cli

import com.codecontext.core.ai.GroundedEvidence
import com.codecontext.core.planner.EngineeringPlanRequest
import com.codecontext.core.planner.EngineeringPlanner
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.default
import java.io.File
import kotlinx.serialization.json.Json

/** Generates a deterministic, evidence-backed engineering plan. */
class EngineeringPlanCommand : CliktCommand(name = "plan") {
    private val changeSummary by argument("change-summary", help = "Short description of the proposed change")
    private val evidenceFile by option("--evidence", help = "Path to a grounded evidence JSON artifact")
        .default("output/grounded-evidence.json")
    private val outputFile by option("--output", help = "Optional output JSON path")

    override fun run() {
        val evidence = Json { ignoreUnknownKeys = true }
            .decodeFromString<GroundedEvidence>(File(evidenceFile).readText())

        val plan = EngineeringPlanner().plan(
            EngineeringPlanRequest(
                changeSummary = changeSummary,
                evidence = evidence
            )
        )
        val json = Json { prettyPrint = true; encodeDefaults = true }
            .encodeToString(plan)

        if (outputFile != null) {
            val output = File(outputFile!!)
            output.parentFile?.mkdirs()
            output.writeText(json)
            echo("Engineering plan: ${output.path}")
        } else {
            echo(json)
        }
    }
}
