package com.codecontext.core.qa

import com.codecontext.core.ai.EvidenceCitation
import com.codecontext.core.ai.GroundedEvidence
import kotlinx.serialization.Serializable

@Serializable
enum class QuestionIntent {
    DEPENDENCY,
    RISK,
    IMPACT,
    ARCHITECTURE,
    PR_CHANGE,
    TEST,
    GENERAL,
    UNKNOWN
}

@Serializable
data class RepositoryQuestion(
    val question: String,
    val intent: QuestionIntent,
    val entity: String? = null
)

@Serializable
data class RetrievedEvidence(
    val citation: EvidenceCitation,
    val relevance: Int
)

@Serializable
data class RepositoryQAResult(
    val question: RepositoryQuestion,
    val evidence: List<RetrievedEvidence>,
    val insufficientEvidence: Boolean
)

object RepositoryQuestionClassifier {
    private val dependencyWords = setOf("depend", "dependency", "dependencies", "uses", "imports", "call", "calls")
    private val riskWords = setOf("risk", "danger", "hotspot", "critical", "risky")
    private val impactWords = setOf("impact", "break", "affected", "affect", "blast radius")
    private val architectureWords = setOf("architecture", "layer", "boundary", "cycle", "module", "coupling")
    private val prWords = setOf("pr", "pull request", "change", "changed", "diff")
    private val testWords = setOf("test", "tests", "testing", "coverage", "verify")

    fun classify(question: String): RepositoryQuestion {
        require(question.isNotBlank()) { "question must not be blank" }
        require(question.length <= 2000) { "question must not exceed 2000 characters" }
        val normalized = question.lowercase()
        val intent = when {
            prWords.any { normalized.contains(it) } -> QuestionIntent.PR_CHANGE
            architectureWords.any { normalized.contains(it) } -> QuestionIntent.ARCHITECTURE
            impactWords.any { normalized.contains(it) } -> QuestionIntent.IMPACT
            riskWords.any { normalized.contains(it) } -> QuestionIntent.RISK
            dependencyWords.any { normalized.contains(it) } -> QuestionIntent.DEPENDENCY
            testWords.any { normalized.contains(it) } -> QuestionIntent.TEST
            normalized.isNotBlank() -> QuestionIntent.GENERAL
            else -> QuestionIntent.UNKNOWN
        }
        return RepositoryQuestion(question.trim(), intent, extractEntity(question))
    }

    private fun extractEntity(question: String): String? =
        Regex("`([^`]+)`").find(question)?.groupValues?.getOrNull(1)
            ?: Regex("\\b[A-Z][A-Za-z0-9_$.]{2,}\\b").find(question)?.value
}

class RepositoryEvidenceRetriever {
    fun retrieve(question: RepositoryQuestion, evidence: GroundedEvidence, maxResults: Int = 8): RepositoryQAResult {
        require(maxResults in 1..32) { "maxResults must be between 1 and 32" }
        val entity = question.entity?.lowercase()
        val ranked = evidence.citations.map { citation ->
            RetrievedEvidence(citation, score(question.intent, entity, citation))
        }.filter { it.relevance > 0 }
            .sortedWith(compareByDescending<RetrievedEvidence> { it.relevance }.thenBy { it.citation.id })
            .take(maxResults)
        return RepositoryQAResult(question, ranked, ranked.isEmpty())
    }

    private fun score(intent: QuestionIntent, entity: String?, citation: EvidenceCitation): Int {
        var score = when (intent) {
            QuestionIntent.RISK -> if (citation.type.contains("hotspot") || citation.type.contains("risk")) 50 else 0
            QuestionIntent.DEPENDENCY -> if (citation.type.contains("file-graph") || citation.type.contains("hotspot")) 45 else 0
            QuestionIntent.ARCHITECTURE -> if (citation.type.contains("architecture")) 60 else if (citation.type.contains("file-graph")) 15 else 0
            QuestionIntent.IMPACT, QuestionIntent.PR_CHANGE -> if (citation.type.contains("file-graph") || citation.type.contains("hotspot")) 30 else 0
            QuestionIntent.TEST -> if (citation.type.contains("file-graph")) 20 else 0
            QuestionIntent.GENERAL -> 10
            QuestionIntent.UNKNOWN -> 0
        }
        if (entity != null && (citation.id.lowercase().contains(entity) || citation.path?.lowercase()?.contains(entity) == true || citation.detail.lowercase().contains(entity))) {
            score += 100
        }
        return score
    }
}
