package com.vericore.core.ai

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class GroundingAssessmentTest : FunSpec({
    test("accepts valid evidence citations and computes full grounding") {
        val evidence = GroundedEvidence(
            citations = listOf(
                EvidenceCitation("repo.metrics", "repository-metrics", detail = "metrics"),
                EvidenceCitation("hotspot.1", "hotspot", path = "src/A.kt", detail = "hotspot")
            )
        )

        val result = GroundingAssessor.assess(
            "The repository contains 10 files [repo.metrics] and src/A.kt is a hotspot [hotspot.1].",
            evidence
        )

        result.citedEvidenceIds shouldBe listOf("repo.metrics", "hotspot.1")
        result.invalidEvidenceIds shouldBe emptyList()
        result.groundingScore shouldBe 1.0
        result.limitations shouldBe emptyList()
    }

    test("zeroes grounding when the answer has no citations") {
        val evidence = GroundedEvidence(
            citations = listOf(EvidenceCitation("repo.metrics", "repository-metrics", detail = "metrics"))
        )

        val result = GroundingAssessor.assess("The repository has a layered architecture.", evidence)

        result.groundingScore shouldBe 0.0
        result.citedEvidenceIds shouldBe emptyList()
        result.limitations shouldContain "AI answer contains no evidence citations."
    }

    test("detects citations that are not present in supplied evidence") {
        val evidence = GroundedEvidence(
            citations = listOf(EvidenceCitation("repo.metrics", "repository-metrics", detail = "metrics"))
        )

        val result = GroundingAssessor.assess(
            "The repository has 20 files [repo.metrics] and a cycle [architecture.cycles].",
            evidence
        )

        result.citedEvidenceIds shouldBe listOf("repo.metrics")
        result.invalidEvidenceIds shouldBe listOf("architecture.cycles")
        result.groundingScore shouldBe 0.5
        result.limitations shouldContain "AI answer cited unavailable evidence IDs: architecture.cycles"
    }

    test("duplicate citations do not inflate the grounding score") {
        val evidence = GroundedEvidence(
            citations = listOf(EvidenceCitation("repo.metrics", "repository-metrics", detail = "metrics"))
        )

        val result = GroundingAssessor.assess(
            "The repository has metrics [repo.metrics] [repo.metrics].",
            evidence
        )

        result.citedEvidenceIds shouldBe listOf("repo.metrics")
        result.groundingScore shouldBe 1.0
    }
})
