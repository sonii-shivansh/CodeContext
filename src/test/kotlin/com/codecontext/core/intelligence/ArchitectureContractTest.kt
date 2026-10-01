package com.codecontext.core.intelligence

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ArchitectureContractTest : FunSpec({
    test("passes when architecture satisfies contract") {
        val architecture = ArchitectureIntelligenceResult(
            "1.0",
            ArchitectureSummary(3, 2, 0, 0, 0, 0),
            emptyList(),
            emptyList(),
            mapOf("api" to 1)
        )
        val result = ArchitectureContractEngine.evaluate(architecture, ArchitectureContract())
        result.passed shouldBe true
        result.violations shouldBe emptyList()
    }

    test("fails when findings exceed contract") {
        val architecture = ArchitectureIntelligenceResult(
            "1.0",
            ArchitectureSummary(3, 2, 1, 0, 1, 0),
            listOf(ArchitectureFinding("ARCH-LAYER-001", "HIGH", "src/A.kt", "src/B.kt", "dependency", "A depends on B")),
            emptyList(),
            mapOf("api" to 1, "domain" to 2)
        )
        val contract = ArchitectureContract(maxFindings = 0, allowedSeverities = listOf("LOW", "MEDIUM"))
        val result = ArchitectureContractEngine.evaluate(architecture, contract)
        result.passed shouldBe false
        result.violations.map { it.ruleId } shouldBe listOf("CONTRACT-FINDING-LIMIT", "CONTRACT-SEVERITY-001")
    }

    test("is deterministic") {
        val architecture = ArchitectureIntelligenceResult(
            "1.0",
            ArchitectureSummary(2, 1, 1, 0, 1, 0),
            listOf(ArchitectureFinding("ARCH-LAYER-001", "HIGH", "src/A.kt", "src/B.kt", "dependency", "A depends on B")),
            emptyList(),
            mapOf("api" to 1, "domain" to 1)
        )
        val contract = ArchitectureContract(maxFindings = 2, allowedSeverities = listOf("LOW", "MEDIUM"))
        val first = ArchitectureContractEngine.evaluate(architecture, contract)
        val second = ArchitectureContractEngine.evaluate(architecture, contract)
        first shouldBe second
    }
})
