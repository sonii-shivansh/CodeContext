package com.vericore.core.evidence

import com.vericore.core.ai.EvidenceCitation
import com.vericore.core.ai.GroundedEvidence
import com.vericore.core.intelligence.AnalysisMetrics
import com.vericore.core.intelligence.AnalysisSnapshot
import com.vericore.core.intelligence.ArchitectureSnapshot
import com.vericore.core.intelligence.RepositorySnapshot
import com.vericore.core.temporal.CodebaseSnapshot
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemanticEvidenceGraphBuilderTest {
    private val repository = "/repo"
    private val commit = "abc123"

    private fun snapshot() = AnalysisSnapshot(
        schemaVersion = "1.1",
        repository = RepositorySnapshot(repository, 1L, listOf("Kotlin"), commit, "state-digest"),
        metrics = AnalysisMetrics(2, 2, 1, false),
        files = emptyList(),
        hotspots = emptyList(),
        architecture = ArchitectureSnapshot(false, 1, 0)
    )

    @Test
    fun `wires grounded citations to repository and source evidence`() {
        val evidence = GroundedEvidence(
            citations = listOf(
                EvidenceCitation("hotspot.1", "hotspot", "src/A.kt", "hotspot", mapOf("score" to "0.5")),
                EvidenceCitation("architecture.summary", "architecture-summary", null, "architecture")
            )
        )

        val graph = SemanticEvidenceGraphBuilder.build(snapshot(), evidence)

        assertEquals(listOf("architecture.summary", "hotspot.1", "repo.state", "source:src/A.kt"), graph.nodes.map { it.id })
        assertEquals(
            setOf(
                EvidenceEdge("architecture.summary", "repo.state", EvidenceEdgeType.DERIVED_FROM),
                EvidenceEdge("hotspot.1", "repo.state", EvidenceEdgeType.DERIVED_FROM),
                EvidenceEdge("hotspot.1", "source:src/A.kt", EvidenceEdgeType.SUPPORTS),
                EvidenceEdge("source:src/A.kt", "repo.state", EvidenceEdgeType.DERIVED_FROM)
            ),
            graph.edges.toSet()
        )
        assertEquals(EvidenceType.HOTSPOT, graph.node("hotspot.1")?.type)
    }

    @Test
    fun `connects architecture findings and temporal history without changing observed state`() {
        val architecture = com.vericore.core.intelligence.ArchitectureIntelligenceResult(
            "1.0",
            com.vericore.core.intelligence.ArchitectureSummary(2, 1, 1, 0, 1, 0),
            listOf(com.vericore.core.intelligence.ArchitectureFinding("ARCH-1", "HIGH", "src/A.kt", null, "dependency", "A depends on B")),
            emptyList(),
            emptyMap()
        )
        val temporal = listOf(CodebaseSnapshot(Instant.parse("2026-01-01T00:00:00Z"), "old123", 2, 20, listOf("src/A.kt")))

        val graph = SemanticEvidenceGraphBuilder.build(snapshot(), architecture = architecture, temporal = temporal)

        assertEquals(commit, graph.observedCommit)
        assertEquals(EvidenceType.ARCHITECTURE, graph.node("architecture.finding.1")?.type)
        assertEquals(EvidenceType.TEMPORAL, graph.node("temporal.old123")?.type)
        assertTrue(graph.edges.contains(EvidenceEdge("architecture.finding.1", "repo.state", EvidenceEdgeType.DERIVED_FROM)))
        assertTrue(graph.edges.contains(EvidenceEdge("temporal.old123", "repo.state", EvidenceEdgeType.DERIVED_FROM)))
    }
}
