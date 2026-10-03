package com.vericore.core.evidence

/**
 * A bounded, deterministic relationship layer over already-produced evidence.
 *
 * The graph does not create repository facts. Callers supply the evidence nodes
 * and explicit relationships between those nodes.
 */
class SemanticEvidenceGraph private constructor(
    val repositoryId: String,
    val observedCommit: String,
    nodes: List<EvidenceNode>,
    edges: List<EvidenceEdge>
) {
    val nodes: List<EvidenceNode> = nodes.sortedBy { it.id }
    val edges: List<EvidenceEdge> = edges.sortedWith(compareBy<EvidenceEdge>({ it.type.name }, { it.fromId }, { it.toId }))

    fun node(id: String): EvidenceNode? = nodes.firstOrNull { it.id == id }

    fun outgoing(id: String): List<EvidenceEdge> = edges.filter { it.fromId == id }

    fun incoming(id: String): List<EvidenceEdge> = edges.filter { it.toId == id }

    /**
     * Resolves evidence produced for the same repository state by a stable source
     * reference. Results are deterministic and optionally constrained by type.
     */
    fun resolve(sourceRef: String, types: Set<EvidenceType> = emptySet()): List<EvidenceNode> =
        nodes.asSequence()
            .filter { it.sourceRef == sourceRef }
            .filter { types.isEmpty() || it.type in types }
            .sortedBy { it.id }
            .toList()

    /**
     * Resolves cross-feature evidence related to a node through a shared source
     * reference. The node itself is excluded from the result.
     */
    fun related(nodeId: String, types: Set<EvidenceType> = emptySet()): List<EvidenceNode> {
        val sourceRef = node(nodeId)?.sourceRef ?: return emptyList()
        return resolve(sourceRef, types).filter { it.id != nodeId }
    }

    companion object {
        fun build(
            repositoryId: String,
            observedCommit: String,
            nodes: List<EvidenceNode>,
            edges: List<EvidenceEdge>
        ): SemanticEvidenceGraph {
            require(repositoryId.isNotBlank()) { "repositoryId must not be blank" }
            require(observedCommit.isNotBlank()) { "observedCommit must not be blank" }

            val duplicateIds = nodes.groupBy { it.id }.filterValues { it.size > 1 }.keys
            require(duplicateIds.isEmpty()) { "Duplicate evidence node IDs: ${duplicateIds.sorted().joinToString()}" }
            require(nodes.all { it.repositoryId == repositoryId && it.observedCommit == observedCommit }) {
                "Every evidence node must belong to the graph repository and observed commit"
            }

            val ids = nodes.mapTo(mutableSetOf()) { it.id }
            edges.forEach { edge ->
                require(edge.fromId in ids) { "Dangling evidence edge source: ${edge.fromId}" }
                require(edge.toId in ids) { "Dangling evidence edge target: ${edge.toId}" }
                require(edge.fromId != edge.toId) { "Self-referential evidence edges are not allowed: ${edge.fromId}" }
            }

            val duplicateEdges = edges.groupBy { it }.filterValues { it.size > 1 }.keys
            require(duplicateEdges.isEmpty()) { "Duplicate evidence edges are not allowed" }

            return SemanticEvidenceGraph(repositoryId, observedCommit, nodes, edges)
        }
    }
}

data class EvidenceNode(
    val id: String,
    val type: EvidenceType,
    val repositoryId: String,
    val observedCommit: String,
    val producer: String,
    val contentDigest: String,
    val sourceRef: String? = null
) {
    init {
        require(id.isNotBlank()) { "Evidence node id must not be blank" }
        require(repositoryId.isNotBlank()) { "Evidence node repositoryId must not be blank" }
        require(observedCommit.isNotBlank()) { "Evidence node observedCommit must not be blank" }
        require(producer.isNotBlank()) { "Evidence node producer must not be blank" }
        require(contentDigest.isNotBlank()) { "Evidence node contentDigest must not be blank" }
    }
}

data class EvidenceEdge(
    val fromId: String,
    val toId: String,
    val type: EvidenceEdgeType
)

enum class EvidenceType {
    REPOSITORY,
    SOURCE,
    ANALYSIS,
    ARCHITECTURE,
    HOTSPOT,
    TEMPORAL,
    REPO_QA,
    GROUNDING
}

enum class EvidenceEdgeType {
    DERIVED_FROM,
    SUPPORTS
}
