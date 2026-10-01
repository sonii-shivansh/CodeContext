package com.codecontext.core.planner

import com.codecontext.core.ai.EvidenceCitation
import com.codecontext.core.ai.GroundedEvidence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EngineeringPlannerTest {
    private val planner = EngineeringPlanner()

    @Test
    fun `plan is deterministic and preserves evidence ids`() {
        val evidence = GroundedEvidence(
            citations = listOf(
                EvidenceCitation("hotspot.1", "hotspot", "src/PaymentService.kt", "central hotspot"),
                EvidenceCitation("architecture.summary", "architecture-summary", null, "cycles=false")
            )
        )
        val request = EngineeringPlanRequest(
            changeSummary = "Update payment validation",
            changedPaths = listOf("src/PaymentService.kt"),
            evidence = evidence
        )

        val first = planner.plan(request)
        val second = planner.plan(request)

        assertEquals(first, second)
        assertEquals(RiskLevel.MEDIUM, first.riskLevel)
        assertEquals(listOf("architecture.summary", "hotspot.1"), first.evidenceIds)
        assertTrue(first.steps.flatMap { it.evidenceIds }.all { it in first.evidenceIds })
    }

    @Test
    fun `empty evidence produces explicit uncertainty`() {
        val plan = planner.plan(
            EngineeringPlanRequest(
                changeSummary = "Unknown change",
                evidence = GroundedEvidence(citations = emptyList())
            )
        )

        assertEquals(RiskLevel.UNKNOWN, plan.riskLevel)
        assertTrue(plan.uncertainties.any { it.contains("No repository evidence") })
        assertTrue(plan.evidenceIds.isEmpty())
    }

    @Test
    fun `limits are enforced`() {
        assertFailsWith<IllegalArgumentException> {
            planner.plan(
                EngineeringPlanRequest(
                    changeSummary = "x",
                    changedPaths = List(501) { "src/$it.kt" },
                    evidence = GroundedEvidence(citations = emptyList())
                )
            )
        }
    }
}
