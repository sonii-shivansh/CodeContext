package com.codecontext.core.ai

import com.codecontext.core.intelligence.AnalysisSnapshot
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

        return GroundedAIResponse(
            answer = response.answer,
            suggestedFiles = response.suggestedFiles,
            confidence = response.confidence,
            evidence = evidence
        )
    }

    private fun buildPrompt(question: String, evidence: GroundedEvidence): String = buildString {
        appendLine("You are answering a repository question using supplied deterministic evidence.")
        appendLine("Treat the evidence below as the authoritative repository facts.")
        appendLine("Do not invent files, metrics, dependencies, architecture facts, or history.")
        appendLine("If the evidence is insufficient, explicitly say what is unknown.")
        appendLine("When making a factual claim, cite one or more evidence IDs like [repo.metrics] or [hotspot.1].")
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
    val evidence: GroundedEvidence
)
