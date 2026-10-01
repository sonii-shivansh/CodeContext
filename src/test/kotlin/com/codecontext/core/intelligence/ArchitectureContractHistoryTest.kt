package com.codecontext.core.intelligence

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class ArchitectureContractHistoryTest : FunSpec({
    test("decision id is deterministic") {
        val root = Files.createTempDirectory("codecontext-contract-history").toFile()
        val contract = ArchitectureContract(maxFindings = 2)
        val architecture = ArchitectureIntelligenceResult(
            "1.0",
            ArchitectureSummary(1, 0, 0, 0, 0, 0),
            emptyList(),
            emptyList(),
            mapOf("api" to 1)
        )
        val result = ArchitectureContractEngine.evaluate(architecture, contract)
        val first = ArchitectureContractHistoryStore.decision(root, contract, result)
        val second = ArchitectureContractHistoryStore.decision(root, contract, result)
        first shouldBe second
    }

    test("record is idempotent and sorted") {
        val root = Files.createTempDirectory("codecontext-contract-history").toFile()
        val file = root.resolve("history.json")
        val contract = ArchitectureContract()
        val architecture = ArchitectureIntelligenceResult(
            "1.0",
            ArchitectureSummary(1, 0, 0, 0, 0, 0),
            emptyList(),
            emptyList(),
            mapOf("api" to 1)
        )
        val result = ArchitectureContractEngine.evaluate(architecture, contract)
        val decision = ArchitectureContractHistoryStore.decision(root, contract, result)
        ArchitectureContractHistoryStore.record(file, decision)
        ArchitectureContractHistoryStore.record(file, decision)
        val history = kotlinx.serialization.json.Json.decodeFromString<ArchitectureContractHistory>(file.readText())
        history.decisions shouldBe listOf(decision)
    }
})
