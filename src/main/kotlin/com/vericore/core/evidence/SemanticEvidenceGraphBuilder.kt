package com.vericore.core.evidence

import com.vericore.core.ai.GroundedEvidence
import com.vericore.core.ai.GroundedEvidenceBuilder
import com.vericore.core.intelligence.AnalysisSnapshot
import com.vericore.core.intelligence.ArchitectureIntelligenceResult
import com.vericore.core.temporal.CodebaseSnapshot
import java.security.MessageDigest

/**
 * Connects existing deterministic evidence producers to the bounded semantic graph.
 *
 * This builder only indexes facts that already exist in producer outputs. It does not
 * infer new repository facts or change the public producer contracts.
 */
object SemanticEvidenceGraphBuilder {
    fun build(
        snapshot: AnalysisSnapshot,
        groundedEvidence: GroundedEvidence = GroundedEvidenceBuilder.fromSnapshot(snapshot),
        architecture: ArchitectureIntelligenceResult? = null,
        temporal: List<CodebaseSnapshot> = emptyList()
    ): SemanticEvidenceGraph {
        val repositoryId = snapshot.repository.path
        val observedCommit = snapshot.repository.repositoryCommit
            ?: error("Semantic evidence graph requires an observed repository commit")

        val nodes = mutableListOf<EvidenceNode>()
        val edges = mutableListOf<EvidenceEdge>()

        nodes += node(
            id = "repo.state",
            type = EvidenceType.REPOSITORY,
            repositoryId = repositoryId,
            observedCommit = observedCommit,
            producer = "AnalysisSnapshotBuilder",
            content = listOf(repositoryId, observedCommit, snapshot.repository.repositoryStateDigest.orEmpty()).joinToString("\u001f")
        )

        groundedEvidence.citations.sortedBy { it.id }.forEach { citation ->
            val type = evidenceType(citation.type)
            nodes += node(
                id = citation.id,
                type = type,
                repositoryId = repositoryId,
                observedCommit = observedCommit,
                producer = "GroundedEvidenceBuilder",
                content = listOf(citation.id, citation.type, citation.path.orEmpty(), citation.detail, citation.metrics.toSortedMap().entries.joinToString(";") { "${it.key}=${it.value}" }).joinToString("\u001f"),
                sourceRef = citation.path
            )
            edges += EvidenceEdge(citation.id, "repo.state", EvidenceEdgeType.DERIVED_FROM)

            citation.path?.takeUnless { it == "<outside-repository>" }?.let { path ->
                val sourceId = "source:$path"
                if (nodes.none { it.id == sourceId }) {
                    nodes += node(
                        id = sourceId,
                        type = EvidenceType.SOURCE,
                        repositoryId = repositoryId,
                        observedCommit = observedCommit,
                        producer = "GroundedEvidenceBuilder",
                        content = path,
                        sourceRef = path
                    )
                    edges += EvidenceEdge(sourceId, "repo.state", EvidenceEdgeType.DERIVED_FROM)
                }
                edges += EvidenceEdge(citation.id, sourceId, EvidenceEdgeType.SUPPORTS)
            }
        }

        architecture?.let { result ->
            result.findings.sortedWith(compareBy({ it.ruleId }, { it.source }, { it.target.orEmpty() }, { it.relationship }, { it.evidence })).forEachIndexed { index, finding ->
                val id = "architecture.finding.${index + 1}"
                nodes += node(
                    id = id,
                    type = EvidenceType.ARCHITECTURE,
                    repositoryId = repositoryId,
                    observedCommit = observedCommit,
                    producer = "ArchitectureIntelligenceEngine",
                    content = listOf(finding.ruleId, finding.severity, finding.source, finding.target.orEmpty(), finding.relationship, finding.evidence).joinToString("\u001f"),
                    sourceRef = finding.source
                )
                edges += EvidenceEdge(id, "repo.state", EvidenceEdgeType.DERIVED_FROM)
                finding.source.takeIf { it.isNotBlank() }?.let { source ->
                    val sourceId = "source:$source"
                    if (nodes.any { it.id == sourceId }) edges += EvidenceEdge(id, sourceId, EvidenceEdgeType.SUPPORTS)
                }
            }
        }

        temporal.sortedBy { it.commitHash }.forEach { history ->
            val id = "temporal.${history.commitHash}"
            nodes += node(
                id = id,
                type = EvidenceType.TEMPORAL,
                repositoryId = repositoryId,
                observedCommit = observedCommit,
                producer = "TemporalAnalyzer",
                content = listOf(history.commitHash, history.timestamp.toString(), history.totalFiles, history.totalLines, history.topHotspots.sorted().joinToString(",")).joinToString("\u001f"),
                sourceRef = history.commitHash
            )
            edges += EvidenceEdge(id, "repo.state", EvidenceEdgeType.DERIVED_FROM)
        }

        return SemanticEvidenceGraph.build(repositoryId, observedCommit, nodes, edges)
    }

    private fun evidenceType(type: String): EvidenceType = when {
        type.contains("architecture", ignoreCase = true) -> EvidenceType.ARCHITECTURE
        type.contains("hotspot", ignoreCase = true) -> EvidenceType.HOTSPOT
        type.contains("temporal", ignoreCase = true) || type.contains("evolution", ignoreCase = true) -> EvidenceType.TEMPORAL
        type.contains("repo-qa", ignoreCase = true) -> EvidenceType.REPO_QA
        type.contains("ground", ignoreCase = true) -> EvidenceType.GROUNDING
        type.contains("file", ignoreCase = true) || type.contains("source", ignoreCase = true) -> EvidenceType.SOURCE
        else -> EvidenceType.ANALYSIS
    }

    private fun node(
        id: String,
        type: EvidenceType,
        repositoryId: String,
        observedCommit: String,
        producer: String,
        content: String,
        sourceRef: String? = null
    ): EvidenceNode = EvidenceNode(
        id = id,
        type = type,
        repositoryId = repositoryId,
        observedCommit = observedCommit,
        producer = producer,
        contentDigest = sha256(content),
        sourceRef = sourceRef
    )

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
