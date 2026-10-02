package com.vericore.cli

import com.vericore.core.ai.GroundedEvidence
import com.vericore.core.planner.EngineeringPlanRequest
import com.vericore.core.planner.EngineeringPlanner
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.multiple
import java.io.File
import kotlinx.serialization.json.Json

/** Generates a deterministic, evidence-backed engineering plan from repository evidence. */
class EngineeringPlanCommand : CliktCommand(
    name = "plan",
    help = "Generate an evidence-backed engineering plan for a proposed change"
) {
    private val changeSummary by argument("change-summary", help = "Short description of the proposed change")
    private val changedPaths by option(
        "--changed",
        help = "Repository-relative file path(s) explicitly planned for the change"
    ).multiple()
    private val evidenceFile by option("--evidence", help = "Path to a grounded evidence JSON artifact")
        .default("output/grounded-evidence.json")
    private val outputFile by option("--output", help = "Write the plan JSON to this path instead of stdout")

    override fun run() {
        val evidence = Json { ignoreUnknownKeys = true }
            .decodeFromString<GroundedEvidence>(File(evidenceFile).readText())

        val plan = EngineeringPlanner().plan(
            EngineeringPlanRequest(
                changeSummary = changeSummary,
                changedPaths = changedPaths.toList(),
                evidence = evidence
            )
        )
        val json = Json { prettyPrint = true; encodeDefaults = true }
            .encodeToString(plan)

        if (outputFile != null) {
            val output = File(outputFile!!)
            output.parentFile?.mkdirs()
            output.writeText(json)
            echo("🧭 Engineering plan: ${output.path}")
            echo("   Risk: ${plan.riskLevel}")
            echo("   Affected components: ${plan.affectedComponents.size}")
            echo("   Planned paths: ${plan.plannedPaths.size}")
        } else {
            echo(json)
        }
    }
}
