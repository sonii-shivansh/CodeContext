package com.vericore.core.ai

/**
 * Deterministic assessment of whether a grounded AI answer cites the evidence it was given.
 * This layer does not judge the model's prose; it only validates repository-evidence references.
 */
data class GroundingAssessment(
    val citedEvidenceIds: List<String>,
    val invalidEvidenceIds: List<String>,
    val groundingScore: Double,
    val limitations: List<String>
)

object GroundingAssessor {
    private val citationPattern = Regex("\\[([A-Za-z0-9._-]+)]")

    fun assess(answer: String, evidence: GroundedEvidence): GroundingAssessment {
        val available = evidence.citations.map { it.id }.toSet()
        val cited = citationPattern
            .findAll(answer)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
        val valid = cited.filter { it in available }
        val invalid = cited.filterNot { it in available }

        val score = when {
            cited.isEmpty() -> 0.0
            valid.isEmpty() -> 0.0
            else -> valid.size.toDouble() / cited.size.toDouble()
        }

        val limitations = buildList {
            if (cited.isEmpty()) {
                add("AI answer contains no evidence citations.")
            }
            if (invalid.isNotEmpty()) {
                add("AI answer cited unavailable evidence IDs: ${invalid.joinToString(", ")}")
            }
            if (valid.isNotEmpty() && valid.size < 2) {
                add("AI answer is grounded in only one evidence citation.")
            }
        }

        return GroundingAssessment(
            citedEvidenceIds = valid,
            invalidEvidenceIds = invalid,
            groundingScore = score,
            limitations = limitations
        )
    }
}
