package com.vericore.core.evidence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class SemanticEvidenceGraphTest {
    private val repositoryId = "https://github.com/example/repo.git"
    private val commit = "abc123"

    private fun node(
        id: String,
        type: EvidenceType = EvidenceType.ANALYSIS,
        sourceRef: String? = null
    ) = EvidenceNode(
        id = id,
        type = type,
        repositoryId = repositoryId,
        observedCommit = commit,
        producer = "test",
        contentDigest = "digest-$id",
        sourceRef = sourceRef
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
    fun `graph digest is deterministic and changes with evidence`() {
        val first = SemanticEvidenceGraph.build(repositoryId, commit, listOf(node("a")), emptyList())
        val second = SemanticEvidenceGraph.build(repositoryId, commit, listOf(node("a")), emptyList())
        val changed = SemanticEvidenceGraph.build(repositoryId, commit, listOf(node("b")), emptyList())

        assertEquals(first.digest(), second.digest())
        assertNotEquals(first.digest(), changed.digest())
    }

    @Test
    fun `graph resolves cross feature evidence by shared source reference`() {
        val graph = SemanticEvidenceGraph.build(
            repositoryId,
            commit,
            nodes = listOf(
                node("architecture.1", EvidenceType.ARCHITECTURE, "src/A.kt"),
                node("hotspot.1", EvidenceType.HOTSPOT, "src/A.kt"),
                node("qa.1", EvidenceType.REPO_QA, "src/A.kt"),
                node("other", EvidenceType.ARCHITECTURE, "src/B.kt")
            ),
            edges = emptyList()
        )

        assertEquals(listOf("architecture.1", "hotspot.1", "qa.1"), graph.resolve("src/A.kt").map { it.id })
        assertEquals(listOf("hotspot.1", "qa.1"), graph.related("architecture.1").map { it.id })
        assertEquals(listOf("hotspot.1"), graph.resolve("src/A.kt", setOf(EvidenceType.HOTSPOT)).map { it.id })
        assertEquals(emptyList(), graph.resolve("src/missing.kt"))
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
