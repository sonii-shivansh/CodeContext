package com.codecontext.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PRIntelligenceEngineTest {
    private fun impact(
        impactedFiles: Int = 0,
        impactedPackages: Int = 0,
        crossPackageImpacts: Int = 0,
        maxDepth: Int = 0,
        changed: List<String> = listOf("src/main/A.kt")
    ) = ChangeImpactResult(
        changedPaths = changed,
        nodes = changed.map { ImpactNode(it, ImpactRelationship.CHANGED, 0, 100.0, listOf("explicitly changed")) },
        summary = ImpactSummary(1, impactedFiles, impactedPackages, crossPackageImpacts, 0, maxDepth)
    )

    @Test
    fun `empty diff produces deterministic informational result`() {
        val result = PRIntelligenceEngine.analyze(ChangeSet(emptyList()), impact = impact(changed = emptyList()))
        assertEquals(0, result.changeSummary.filesChanged)
        assertEquals(FindingSeverity.INFO, result.aggregateSeverity)
        assertTrue(result.findings.isEmpty())
        assertEquals("1.0", result.schemaVersion)
    }

    @Test
    fun `unresolved deleted file is preserved as finding`() {
        val result = PRIntelligenceEngine.analyze(
            ChangeSet(listOf(ChangedFile("src/main/Removed.kt", ChangeType.DELETED, deletions = 40))),
            impact = impact(changed = emptyList())
        )
        assertEquals(1, result.changeSummary.filesDeleted)
        assertTrue(result.findings.any { it.ruleId == "CHANGE_UNRESOLVED" })
    }

    @Test
    fun `broad cross package high risk change creates explainable findings`() {
        val result = PRIntelligenceEngine.analyze(
            ChangeSet(listOf(ChangedFile("src/main/A.kt", ChangeType.MODIFIED, additions = 50, deletions = 10))),
            impact = impact(impactedFiles = 30, impactedPackages = 4, crossPackageImpacts = 12, maxDepth = 5),
            risks = listOf(EngineeringRisk("src/main/A.kt", 85.0, RiskLevel.CRITICAL, listOf("high dependency centrality"))),
            packageByPath = mapOf("src/main/A.kt" to "a")
        )
        assertEquals(FindingSeverity.HIGH, result.aggregateSeverity)
        assertTrue(result.findings.any { it.ruleId == "IMPACT_BROAD" && it.severity == FindingSeverity.HIGH })
        assertTrue(result.findings.any { it.ruleId == "ARCH_CROSS_PACKAGE" })
        assertTrue(result.findings.any { it.ruleId == "CHANGED_HIGH_RISK_COMPONENT" })
    }

    @Test
    fun `missing test candidates is a review signal`() {
        val result = PRIntelligenceEngine.analyze(
            ChangeSet(listOf(ChangedFile("src/main/Service.kt", ChangeType.MODIFIED))),
            impact = impact()
        )
        assertTrue(result.findings.any { it.ruleId == "TEST_CANDIDATE_MISSING" })
    }

    @Test
    fun `large changes produce size finding`() {
        val files = (1..20).map { ChangedFile("src/main/F$it.kt", ChangeType.MODIFIED, additions = 30) }
        val result = PRIntelligenceEngine.analyze(ChangeSet(files), impact = impact())
        assertTrue(result.findings.any { it.ruleId == "CHANGE_LARGE" })
        assertEquals(600, result.changeSummary.additions)
    }

    @Test
    fun `findings are deterministically ordered`() {
        val changeSet = ChangeSet(listOf(ChangedFile("src/main/A.kt", ChangeType.MODIFIED)))
        val first = PRIntelligenceEngine.analyze(changeSet, impact(impactedFiles = 10, crossPackageImpacts = 1, maxDepth = 3))
        val second = PRIntelligenceEngine.analyze(changeSet, impact(impactedFiles = 10, crossPackageImpacts = 1, maxDepth = 3))
        assertEquals(first, second)
        assertEquals(first.findings.map { it.ruleId }, listOf("ARCH_CROSS_PACKAGE", "IMPACT_BROAD", "TEST_CANDIDATE_MISSING"))
    }
}
