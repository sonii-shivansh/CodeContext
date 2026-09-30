package com.codecontext.core

import com.codecontext.core.intelligence.ANALYSIS_SCHEMA_VERSION
import com.codecontext.core.intelligence.AnalysisMetrics
import com.codecontext.core.intelligence.AnalysisSnapshot
import com.codecontext.core.intelligence.ArchitectureSnapshot
import com.codecontext.core.intelligence.EngineeringRiskEngine
import com.codecontext.core.intelligence.FileSnapshot
import com.codecontext.core.intelligence.HotspotSnapshot
import com.codecontext.core.intelligence.RepositorySnapshot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AnalysisSnapshotTest : FunSpec({
    test("risk engine is deterministic for identical snapshots") {
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

        first.size shouldBe 2
        first shouldBe second
        first.first().path shouldBe "/repo/A.kt"
    }
})
