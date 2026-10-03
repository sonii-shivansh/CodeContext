package com.vericore.core.ai

import com.vericore.core.intelligence.AnalysisSnapshot
import java.io.File

/**
 * Orchestrates grounded questions without changing the provider abstraction.
 * Deterministic evidence is embedded into the question sent to the existing AI analyzer.
 */
class GroundedAIService(private val analyzer: AICodeAnalyzer) {
    suspend fun ask(
        question: String,
        snapshot: AnalysisSnapshot,
        maxCitations: Int = 24
    ): GroundedAIResponse {
        require(question.isNotBlank()) { "question must not be blank" }

        val evidence = GroundedEvidenceBuilder.fromSnapshot(snapshot, maxCitations)
        val context = CodebaseContext(
            totalFiles = snapshot.metrics.totalFiles,
            languages = snapshot.repository.languages,
            hotspots = snapshot.hotspots.map { it.path },
            recentChanges = emptyList()
        )

        val groundedQuestion = buildPrompt(question, evidence)
        val response = analyzer.askQuestion(groundedQuestion, context)
        val grounding = GroundingAssessor.assess(response.answer, evidence)

        return GroundedAIResponse(
            answer = response.answer,
            suggestedFiles = response.suggestedFiles,
            confidence = minOf(response.confidence.coerceIn(0.0, 1.0), grounding.groundingScore),
            evidence = evidence,
            citedEvidenceIds = grounding.citedEvidenceIds,
            groundingScore = grounding.groundingScore,
            limitations = grounding.limitations
        )
    }

    private fun buildPrompt(question: String, evidence: GroundedEvidence): String = buildString {
        appendLine("You are answering a repository question using supplied deterministic evidence.")
        appendLine("Treat the evidence below as the authoritative repository facts.")
        appendLine("Do not invent files, metrics, dependencies, architecture facts, or history.")
        appendLine("If the evidence is insufficient, explicitly say what is unknown.")
        appendLine("Every repository-specific factual claim MUST cite one or more evidence IDs like [repo.metrics] or [hotspot.1].")
        appendLine("Do not cite an evidence ID that is not present below.")
        appendLine()
        appendLine("DETERMINISTIC EVIDENCE")
        evidence.citations.forEach { citation ->
            appendLine("[${citation.id}] ${citation.type}: ${citation.detail}")
            citation.path?.let { appendLine("  path: ${normalizePath(it)}") }
            if (citation.metrics.isNotEmpty()) {
                appendLine("  metrics: ${citation.metrics.toSortedMap().entries.joinToString(", ") { "${it.key}=${it.value}" }}")
            }
        }
        appendLine()
        appendLine("DEVELOPER QUESTION")
        appendLine(question.replace("```", ""))
    }

    private fun normalizePath(path: String): String = File(path).path.replace('\\', '/')
}

@kotlinx.serialization.Serializable
data class GroundedAIResponse(
    val answer: String,
    val suggestedFiles: List<String>,
    val confidence: Double,
    val evidence: GroundedEvidence,
    val citedEvidenceIds: List<String> = emptyList(),
    val groundingScore: Double = 0.0,
    val limitations: List<String> = emptyList()
)
