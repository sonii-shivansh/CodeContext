package com.codecontext.core

import com.codecontext.core.intelligence.AnalysisMetrics
import com.codecontext.core.intelligence.AnalysisSnapshot
import com.codecontext.core.intelligence.AnalysisSnapshotBuilder
import com.codecontext.core.intelligence.ArchitectureSnapshot
import com.codecontext.core.intelligence.FileSnapshot
import com.codecontext.core.intelligence.HotspotSnapshot
import com.codecontext.core.intelligence.RepositorySnapshot
import com.codecontext.core.intelligence.EngineeringRiskEngine
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AnalysisSnapshotTest : StringSpec({
    "risk engine should be deterministic for identical snapshots" {
        val snapshot = AnalysisSnapshot(
            repository = RepositorySnapshot("/repo", 1L, listOf("Kotlin")),
            metrics = AnalysisMetrics(2, 2, 1, 0),
            files = listOf(
                FileSnapshot("/repo/A.kt", "a", 20, 12, listOf("dev"), 0.03),
                FileSnapshot("/repo/B.kt", "b", 2, 0, listOf("dev"), 0.001)
            ),
            hotspots = listOf(HotspotSnapshot("/repo/A.kt", 0.03, 12, 11, 2)),
            architecture = ArchitectureSnapshot(false, 2, 1)
        )

        val first = EngineeringRiskEngine.calculate(snapshot)
        val second = EngineeringRiskEngine.calculate(snapshot)

        first shouldBe second
        first.first().path shouldBe "/repo/A.kt"
    }
})
