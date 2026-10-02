package com.codecontext.core.workflow

import com.codecontext.core.planner.EngineeringPlan
import kotlinx.serialization.Serializable
import java.io.File
import java.security.MessageDigest

@Serializable
data class AgentChangeContract(
    val schemaVersion: String = "2.0",
    val changeSummary: String,
    val repository: String,
    val preparedHead: String,
    val plannedPaths: List<String>,
    val expectedChangeTypes: Map<String, List<String>> = emptyMap(),
    val expectedComponents: List<String>,
    val verificationCommands: List<String>,
    val evidenceIds: List<String>,
    val architectureExpectations: List<String> = emptyList(),
    val fingerprint: String
) {
    companion object {
        fun fromPlan(plan: EngineeringPlan): AgentChangeContract =
            fromPlan(plan, "", "")

        fun fromPlan(plan: EngineeringPlan, repositoryPath: String, preparedHead: String): AgentChangeContract {
            val repository = repositoryPath.takeIf { it.isNotBlank() }?.let { File(it).canonicalPath } ?: ""
            val paths = plan.plannedPaths.map(::normalize).distinct().sorted()
            val components = plan.affectedComponents.map(::normalize).distinct().sorted()
            val commands = plan.verificationCommands.distinct().sorted()
            val evidence = plan.evidenceIds.distinct().sorted()
            val expectations = plan.concerns.distinct().sorted()
            val canonical = canonicalize(plan.changeSummary, repository, preparedHead, paths, components, commands, evidence, expectations)
            return AgentChangeContract(
                changeSummary = plan.changeSummary.trim(),
                repository = repository,
                preparedHead = preparedHead,
                plannedPaths = paths,
                expectedComponents = components,
                verificationCommands = commands,
                evidenceIds = evidence,
                architectureExpectations = expectations,
                fingerprint = sha256(canonical)
            )
        }

        fun fingerprintFor(plan: EngineeringPlan): String = fromPlan(plan).fingerprint

        fun fingerprintFor(contract: AgentChangeContract): String = sha256(
            canonicalize(contract.changeSummary, contract.repository, contract.preparedHead, contract.plannedPaths.map(::normalize).distinct().sorted(), contract.expectedComponents.map(::normalize).distinct().sorted(), contract.verificationCommands.distinct().sorted(), contract.evidenceIds.distinct().sorted(), contract.architectureExpectations.distinct().sorted())
        )

        private fun canonicalize(summary: String, repository: String, preparedHead: String, paths: List<String>, components: List<String>, commands: List<String>, evidence: List<String>, expectations: List<String>): String =
            listOf(summary.trim(), repository, preparedHead, paths.joinToString("\n"), components.joinToString("\n"), commands.joinToString("\n"), evidence.joinToString("\n"), expectations.joinToString("\n")).joinToString("\n---\n")

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
