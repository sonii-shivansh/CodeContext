package com.vericore.core.intelligence

import kotlinx.serialization.Serializable

@Serializable
data class ArchitectureContract(
    val schemaVersion: String = "1.0",
    val maxFindings: Int = Int.MAX_VALUE,
    val maxCycles: Int = Int.MAX_VALUE,
    val allowedSeverities: List<String> = listOf("LOW", "MEDIUM", "HIGH", "CRITICAL"),
    val requiredRules: List<String> = emptyList()
)

@Serializable
data class ArchitectureContractResult(
    val schemaVersion: String,
    val passed: Boolean,
    val violations: List<ArchitectureContractViolation>,
    val checked: ArchitectureContract
)

@Serializable
data class ArchitectureContractViolation(
    val ruleId: String,
    val severity: String,
    val message: String,
    val source: String? = null,
    val target: String? = null
)

object ArchitectureContractEngine {
    fun evaluate(
        result: ArchitectureIntelligenceResult,
        contract: ArchitectureContract
    ): ArchitectureContractResult {
        val violations = mutableListOf<ArchitectureContractViolation>()

        if (result.findings.size > contract.maxFindings) {
            violations += ArchitectureContractViolation(
                "CONTRACT-FINDING-LIMIT",
                "HIGH",
                "Architecture findings ${result.findings.size} exceed contract maximum ${contract.maxFindings}"
            )
        }
        if (result.cycles.size > contract.maxCycles) {
            violations += ArchitectureContractViolation(
                "CONTRACT-CYCLE-LIMIT",
                "HIGH",
                "Architecture cycles ${result.cycles.size} exceed contract maximum ${contract.maxCycles}"
            )
        }

        val allowed = contract.allowedSeverities.toSet()
        result.findings.filter { it.severity !in allowed }.forEach { finding ->
            violations += ArchitectureContractViolation(
                "CONTRACT-SEVERITY-001",
                finding.severity,
                "Finding severity ${finding.severity} is not allowed by the contract",
                finding.source,
                finding.target
            )
        }

        val foundRules = result.findings.map { it.ruleId }.toSet()
        contract.requiredRules.filter { it !in foundRules }.forEach { ruleId ->
            violations += ArchitectureContractViolation(
                "CONTRACT-RULE-001",
                "HIGH",
                "Required architecture rule $ruleId was not observed"
            )
        }

        val sorted = violations.distinct().sortedWith(
            compareBy<ArchitectureContractViolation> { it.severityOrder() }
                .thenBy { it.ruleId }
                .thenBy { it.source.orEmpty() }
                .thenBy { it.target.orEmpty() }
                .thenBy { it.message }
        )
        return ArchitectureContractResult("1.0", sorted.isEmpty(), sorted, contract)
    }

    private fun ArchitectureContractViolation.severityOrder(): Int = when (severity) {
        "CRITICAL" -> 0
        "HIGH" -> 1
        "MEDIUM" -> 2
        "LOW" -> 3
        else -> 4
    }
}
