package com.codecontext.core.reality

import com.codecontext.core.intelligence.AnalysisMetrics
import com.codecontext.core.intelligence.AnalysisSnapshot
import com.codecontext.core.intelligence.ArchitectureSnapshot
import com.codecontext.core.intelligence.EngineeringContextSnapshot
import com.codecontext.core.intelligence.FileSnapshot
import com.codecontext.core.intelligence.HotspotSnapshot
import com.codecontext.core.intelligence.RepositorySnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class EngineeringRealityTest {
    @Test
    fun `reality digest is deterministic for the same artifacts`() {
        val analysis = sampleAnalysis()
        val context = sampleContext()
        val first = EngineeringRealityEngine.build(analysis, context)
        val second = EngineeringRealityEngine.build(analysis, context)
        assertEquals(first, second)
        assertEquals(first.realityDigest, second.realityDigest)
    }

    @Test
    fun `analysis timestamp does not change reality identity`() {
        val first = EngineeringRealityEngine.build(sampleAnalysis(), sampleContext())
        val laterAnalysis = sampleAnalysis().copy(
            repository = sampleAnalysis().repository.copy(analyzedAtEpochMillis = 999999L)
        )
        val second = EngineeringRealityEngine.build(laterAnalysis, sampleContext())
        assertEquals(first.realityDigest, second.realityDigest)
    }

    @Test
    fun `reality digest changes when repository state changes`() {
        val analysis = sampleAnalysis()
        val first = EngineeringRealityEngine.build(analysis, sampleContext())
        val changed = EngineeringRealityEngine.build(
            analysis.copy(repository = analysis.repository.copy(repositoryStateDigest = null)),
            sampleContext().copy(changedPaths = listOf("src/New.kt"))
        )
        assertNotEquals(first.realityDigest, changed.realityDigest)
    }

    @Test
    fun `stale analysis commit is rejected`() {
        val stale = sampleAnalysis().copy(
            repository = sampleAnalysis().repository.copy(repositoryCommit = "old-commit")
        )
        assertFailsWith<IllegalArgumentException> {
            EngineeringRealityEngine.build(stale, sampleContext())
        }
    }

    @Test
    fun `stale analysis source state is rejected`() {
        val stale = sampleAnalysis().copy(
            repository = sampleAnalysis().repository.copy(repositoryStateDigest = "old-state")
        )
        assertFailsWith<IllegalArgumentException> {
            EngineeringRealityEngine.build(stale, sampleContext())
        }
    }

    private fun sampleAnalysis() = AnalysisSnapshot(
        schemaVersion = "1.1",
        repository = RepositorySnapshot(
            path = "/repo",
            analyzedAtEpochMillis = 1L,
            languages = listOf("Java", "Kotlin")
        ),
        metrics = AnalysisMetrics(2, 2, 1, false, 0),
        files = listOf(
            FileSnapshot("/repo/A.java", "demo", 1, 2, listOf("alice"), 0.7, 1, 0),
            FileSnapshot("/repo/B.kt", "demo", 1, 1, listOf("bob"), 0.3, 0, 1)
        ),
        hotspots = listOf(HotspotSnapshot("/repo/A.java", 0.7, 2, 1, 0)),
        architecture = ArchitectureSnapshot(false, 1, 0)
    )

    private fun sampleContext() = EngineeringContextSnapshot(
        schemaVersion = "1.0",
        repositoryCommit = "abc123",
        files = emptyList(),
        totalFiles = 2,
        totalBytes = 100,
        languages = listOf("Java", "Kotlin"),
        dirty = false,
        changedPaths = emptyList(),
        snapshotDigest = "context"
    )
}
