package com.codecontext.core

import com.codecontext.core.intelligence.ANALYSIS_SCHEMA_VERSION
import com.codecontext.core.intelligence.AnalysisMetrics
import com.codecontext.core.intelligence.AnalysisSnapshot
import com.codecontext.core.intelligence.ArchitectureSnapshot
import com.codecontext.core.intelligence.EngineeringRiskEngine
import com.codecontext.core.intelligence.FileSnapshot
import com.codecontext.core.intelligence.HotspotSnapshot
import com.codecontext.core.intelligence.RepositorySnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalysisSnapshotTest {
    @Test
    fun riskEngineIsDeterministicForIdenticalSnapshots() {
        val snapshot = AnalysisSnapshot(
            schemaVersion = ANALYSIS_SCHEMA_VERSION,
            repository = RepositorySnapshot("/repo", 1L, listOf("Kotlin")),
            metrics = AnalysisMetrics(2, 2, 1, 0),
            files = listOf(
                FileSnapshot("/repo/A.kt", "a", 20, 12, listOf("dev"), 0.03, 11, 2),
                FileSnapshot("/repo/B.kt", "b", 2, 0, listOf("dev"), 0.001, 0, 0)
            ),
            hotspots = listOf(HotspotSnapshot("/repo/A.kt", 0.03, 12, 11, 2)),
            architecture = ArchitectureSnapshot(false, 2, 1)
        )

        val first = EngineeringRiskEngine.calculate(snapshot)
        val second = EngineeringRiskEngine.calculate(snapshot)

        assertEquals(2, first.size)
        assertEquals(first, second)
        assertEquals("/repo/A.kt", first.first().path)
    }
}
