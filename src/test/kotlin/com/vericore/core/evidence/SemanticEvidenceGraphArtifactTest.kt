package com.vericore.core.evidence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SemanticEvidenceGraphArtifactTest {
    private val repositoryId = "/repo"
    private val commit = "0123456789abcdef"

    @Test
    fun `artifact preserves deterministic graph identity and ordering`() {
        val graph = SemanticEvidenceGraph.build(
            repositoryId = repositoryId,
            observedCommit = commit,
            nodes = listOf(
                EvidenceNode("source:b", EvidenceType.SOURCE, repositoryId, commit, "test", "b", "src/B.java"),
                EvidenceNode("repo.state", EvidenceType.REPOSITORY, repositoryId, commit, "test", "a"),
                EvidenceNode("source:a", EvidenceType.SOURCE, repositoryId, commit, "test", "a", "src/A.java")
            ),
            edges = listOf(
                EvidenceEdge("source:b", "repo.state", EvidenceEdgeType.DERIVED_FROM),
                EvidenceEdge("source:a", "repo.state", EvidenceEdgeType.DERIVED_FROM)
            )
        )

        val artifact = SemanticEvidenceGraphArtifact.fromGraph(graph)
        val decoded = SemanticEvidenceGraphArtifactCodec.decode(SemanticEvidenceGraphArtifactCodec.encode(artifact))

        assertEquals(SemanticEvidenceGraphArtifact.SCHEMA_VERSION, artifact.schemaVersion)
        assertEquals(3, artifact.nodeCount)
        assertEquals(2, artifact.edgeCount)
        assertEquals(graph.digest(), artifact.digest)
        assertEquals(artifact, decoded)
        assertEquals(listOf("repo.state", "source:a", "source:b"), artifact.nodes.map { it.id })
    }

    @Test
    fun `artifact rejects inconsistent counts`() {
        val node = EvidenceNode("repo.state", EvidenceType.REPOSITORY, repositoryId, commit, "test", "digest")
        assertFailsWith<IllegalArgumentException> {
            SemanticEvidenceGraphArtifact(
                schemaVersion = SemanticEvidenceGraphArtifact.SCHEMA_VERSION,
                repositoryId = repositoryId,
                observedCommit = commit,
                digest = "digest",
                nodeCount = 2,
                edgeCount = 0,
                nodes = listOf(node),
                edges = emptyList()
            )
        }
    }
}
