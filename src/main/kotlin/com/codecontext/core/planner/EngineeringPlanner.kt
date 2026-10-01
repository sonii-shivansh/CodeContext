package com.codecontext.core.planner

import com.codecontext.core.ai.EvidenceCitation
import com.codecontext.core.ai.GroundedEvidence
import kotlinx.serialization.Serializable

@Serializable
data class EngineeringPlanRequest(
    val changeSummary: String,
    val changedPaths: List<String> = emptyList(),
    val evidence: GroundedEvidence
)

@Serializable
data class EngineeringPlanStep(
    val id: String,
    val description: String,
    val rationale: String,
    val evidenceIds: List<String> = emptyList(),
    val verification: String
)

@Serializable
data class EngineeringPlan(
    val schemaVersion: String = "1.0",
    val changeSummary: String,
    val affectedComponents: List<String>,
    val concerns: List<String>,
    val riskLevel: RiskLevel,
    val steps: List<EngineeringPlanStep>,
    val verificationCommands: List<String>,
    val evidenceIds: List<String>,
    val uncertainties: List<String>
)

@Serializable
enum class RiskLevel { LOW, MEDIUM, HIGH, UNKNOWN }

/** Builds a bounded, evidence-backed implementation plan without requiring an AI provider. */
class EngineeringPlanner {
    fun plan(request: EngineeringPlanRequest): EngineeringPlan {
        require(request.changeSummary.isNotBlank()) { "changeSummary must not be blank" }
        require(request.changeSummary.length <= 4000) { "changeSummary must not exceed 4000 characters" }
        require(request.changedPaths.size <= 500) { "changedPaths must not exceed 500 entries" }

        val citations = request.evidence.citations.sortedBy { it.id }
        val affected = (request.changedPaths + citations.mapNotNull { it.path })
            .map { normalizePath(it) }
            .filter { it.isNotEmpty() && !it.startsWith("<outside-") && !isGeneratedPath(it) }
            .distinct()
            .sorted()
            .take(100)

        val architecture = citations.filter { it.type.contains("architecture") }
        val hotspots = citations.filter { it.type.contains("hotspot") }
        val concerns = buildList {
            if (architecture.isNotEmpty()) add("Review architecture evidence before implementation.")
            if (hotspots.isNotEmpty()) add("Changed or related components include dependency-centrality hotspots.")
        }

        val risk = when {
            citations.any { it.type.contains("critical") } -> RiskLevel.HIGH
            hotspots.isNotEmpty() || architecture.isNotEmpty() -> RiskLevel.MEDIUM
            citations.isNotEmpty() -> RiskLevel.LOW
            else -> RiskLevel.UNKNOWN
        }

        val evidenceIds = citations.map { it.id }.distinct().sorted()
        val steps = buildList {
            add(
                EngineeringPlanStep(
                    id = "step-1",
                    description = "Review the proposed change against the affected components.",
                    rationale = "Establish the concrete repository scope before implementation.",
                    evidenceIds = evidenceIds.take(8),
                    verification = "Confirm every changed path belongs to the intended change scope."
                )
            )
            if (architecture.isNotEmpty()) add(
                EngineeringPlanStep(
                    id = "step-2",
                    description = "Review architecture boundaries and dependency direction around the change.",
                    rationale = "Architecture evidence indicates structural constraints that may affect the implementation.",
                    evidenceIds = architecture.map { it.id }.sorted(),
                    verification = "Run architecture analysis and confirm no new forbidden dependency is introduced."
                )
            )
            if (hotspots.isNotEmpty()) add(
                EngineeringPlanStep(
                    id = "step-${size + 1}",
                    description = "Review hotspot dependencies and downstream consumers before changing shared components.",
                    rationale = "High-centrality components can expand the change blast radius.",
                    evidenceIds = hotspots.map { it.id }.sorted(),
                    verification = "Run impact analysis and inspect affected dependents."
                )
            )
            add(
                EngineeringPlanStep(
                    id = "step-${size + 1}",
                    description = "Implement the smallest change that satisfies the requested behavior.",
                    rationale = "Keep the change bounded to the evidence-supported scope.",
                    evidenceIds = evidenceIds.take(8),
                    verification = "Run the project's unit and integration test suite."
                )
            )
        }

        val uncertainties = buildList {
            if (citations.isEmpty()) add("No repository evidence was supplied; implementation-specific conclusions cannot be established.")
            if (request.changedPaths.isEmpty()) add("No explicit changed paths were supplied; affected components are inferred only from available evidence.")
        }

        return EngineeringPlan(
            changeSummary = request.changeSummary.trim(),
            affectedComponents = affected,
            concerns = concerns.sorted(),
            riskLevel = risk,
            steps = steps,
            verificationCommands = listOf("./gradlew --no-daemon clean test", "./gradlew --no-daemon build installDist"),
            evidenceIds = evidenceIds,
            uncertainties = uncertainties
        )
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/').trim().removePrefix("./")

    private fun isGeneratedPath(path: String): Boolean {
        val normalized = normalizePath(path).trimStart('/')
        return normalized == ".codecontext" || normalized.startsWith(".codecontext/") ||
            normalized == "output" || normalized.startsWith("output/") ||
            normalized == "build" || normalized.startsWith("build/") ||
            normalized == "target" || normalized.startsWith("target/")
    }
}
