package com.vericore.core.evidence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SemanticEvidenceGraphTest {
    private val repositoryId = "https://github.com/example/repo.git"
    private val commit = "abc123"

    private fun node(id: String, type: EvidenceType = EvidenceType.ANALYSIS) = EvidenceNode(
        id = id,
        type = type,
        repositoryId = repositoryId,
        observedCommit = commit,
        producer = "test",
        contentDigest = "digest-$id"
    )

    @Test
    fun `graph sorts nodes and edges deterministically`() {
        val graph = SemanticEvidenceGraph.build(
            repositoryId,
            commit,
            nodes = listOf(node("b"), node("a")),
            edges = listOf(
                EvidenceEdge("b", "a", EvidenceEdgeType.SUPPORTS),
                EvidenceEdge("a", "b", EvidenceEdgeType.DERIVED_FROM)
            )
        )

        assertEquals(listOf("a", "b"), graph.nodes.map { it.id })
        assertEquals(
            listOf(
                EvidenceEdge("a", "b", EvidenceEdgeType.DERIVED_FROM),
                EvidenceEdge("b", "a", EvidenceEdgeType.SUPPORTS)
            ),
            graph.edges
        )
    }

    @Test
    fun `graph rejects duplicate node ids`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticEvidenceGraph.build(repositoryId, commit, listOf(node("a"), node("a")), emptyList())
        }
    }

    @Test
    fun `graph rejects cross repository evidence`() {
        val foreign = node("foreign").copy(repositoryId = "https://github.com/other/repo.git")
        assertFailsWith<IllegalArgumentException> {
            SemanticEvidenceGraph.build(repositoryId, commit, listOf(node("a"), foreign), emptyList())
        }
    }

    @Test
    fun `graph rejects stale commit evidence`() {
        val stale = node("stale").copy(observedCommit = "old")
        assertFailsWith<IllegalArgumentException> {
            SemanticEvidenceGraph.build(repositoryId, commit, listOf(node("a"), stale), emptyList())
        }
    }

    @Test
    fun `graph rejects dangling edges`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticEvidenceGraph.build(
                repositoryId,
                commit,
                listOf(node("a")),
                listOf(EvidenceEdge("a", "missing", EvidenceEdgeType.SUPPORTS))
            )
        }
    }

    @Test
    fun `graph rejects self relationships and duplicate edges`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticEvidenceGraph.build(
                repositoryId,
                commit,
                listOf(node("a")),
                listOf(EvidenceEdge("a", "a", EvidenceEdgeType.SUPPORTS))
            )
        }

        val edge = EvidenceEdge("a", "b", EvidenceEdgeType.SUPPORTS)
        assertFailsWith<IllegalArgumentException> {
            SemanticEvidenceGraph.build(repositoryId, commit, listOf(node("a"), node("b")), listOf(edge, edge))
        }
    }

    @Test
    fun `empty graph is valid`() {
        val graph = SemanticEvidenceGraph.build(repositoryId, commit, emptyList(), emptyList())
        assertEquals(emptyList(), graph.nodes)
        assertEquals(emptyList(), graph.edges)
    }
}
