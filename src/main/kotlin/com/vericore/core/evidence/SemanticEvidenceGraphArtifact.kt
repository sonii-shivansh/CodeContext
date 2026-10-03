package com.vericore.core.evidence

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Stable machine-readable representation of the semantic evidence graph.
 *
 * The artifact is derived only from deterministic evidence already present in the
 * graph. It is safe to persist, inspect, and compare without exposing mutable
 * graph internals.
 */
@Serializable
data class SemanticEvidenceGraphArtifact(
    val schemaVersion: String,
    val repositoryId: String,
    val observedCommit: String,
    val digest: String,
    val nodeCount: Int,
    val edgeCount: Int,
    val nodes: List<EvidenceNode>,
    val edges: List<EvidenceEdge>
) {
    init {
        require(schemaVersion.isNotBlank()) { "schemaVersion must not be blank" }
        require(repositoryId.isNotBlank()) { "repositoryId must not be blank" }
        require(observedCommit.isNotBlank()) { "observedCommit must not be blank" }
        require(digest.isNotBlank()) { "digest must not be blank" }
        require(nodeCount == nodes.size) { "nodeCount does not match nodes" }
        require(edgeCount == edges.size) { "edgeCount does not match edges" }
    }

    companion object {
        const val SCHEMA_VERSION = "1.0"

        fun fromGraph(graph: SemanticEvidenceGraph): SemanticEvidenceGraphArtifact =
            SemanticEvidenceGraphArtifact(
                schemaVersion = SCHEMA_VERSION,
                repositoryId = graph.repositoryId,
                observedCommit = graph.observedCommit,
                digest = graph.digest(),
                nodeCount = graph.nodes.size,
                edgeCount = graph.edges.size,
                nodes = graph.nodes,
                edges = graph.edges
            )
    }
}

object SemanticEvidenceGraphArtifactCodec {
    private val json = Json { encodeDefaults = true; prettyPrint = true }

    fun encode(artifact: SemanticEvidenceGraphArtifact): String =
        json.encodeToString(SemanticEvidenceGraphArtifact.serializer(), artifact)

    fun decode(value: String): SemanticEvidenceGraphArtifact =
        json.decodeFromString(SemanticEvidenceGraphArtifact.serializer(), value)
}
