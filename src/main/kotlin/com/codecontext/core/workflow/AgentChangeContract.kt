package com.codecontext.core.workflow

import com.codecontext.core.planner.EngineeringPlan
import kotlinx.serialization.Serializable
import java.security.MessageDigest

@Serializable
data class AgentChangeContract(
    val schemaVersion: String = "1.0",
    val changeSummary: String,
    val plannedPaths: List<String>,
    val expectedChangeTypes: Map<String, List<String>> = emptyMap(),
    val expectedComponents: List<String>,
    val verificationCommands: List<String>,
    val evidenceIds: List<String>,
    val architectureExpectations: List<String> = emptyList(),
    val fingerprint: String
) {
    companion object {
        fun fromPlan(plan: EngineeringPlan): AgentChangeContract {
            val paths = plan.plannedPaths.map(::normalize).distinct().sorted()
            val components = plan.affectedComponents.map(::normalize).distinct().sorted()
            val commands = plan.verificationCommands.distinct().sorted()
            val evidence = plan.evidenceIds.distinct().sorted()
            val expectations = plan.concerns.distinct().sorted()
            val canonical = canonicalize(plan.changeSummary, paths, components, commands, evidence, expectations)
            return AgentChangeContract(
                changeSummary = plan.changeSummary.trim(),
                plannedPaths = paths,
                expectedComponents = components,
                verificationCommands = commands,
                evidenceIds = evidence,
                architectureExpectations = expectations,
                fingerprint = sha256(canonical)
            )
        }

        fun fingerprintFor(plan: EngineeringPlan): String = fromPlan(plan).fingerprint

        private fun canonicalize(summary: String, paths: List<String>, components: List<String>, commands: List<String>, evidence: List<String>, expectations: List<String>): String =
            listOf(summary.trim(), paths.joinToString("\n"), components.joinToString("\n"), commands.joinToString("\n"), evidence.joinToString("\n"), expectations.joinToString("\n")).joinToString("\n---\n")

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        private fun normalize(path: String): String = path.replace('\\', '/').trim().removePrefix("./")
    }
}

@Serializable
data class AgentChangeContractResult(
    val contract: AgentChangeContract,
    val valid: Boolean,
    val reasons: List<String> = emptyList()
)
