package com.vericore.core.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class ArchitectureContractFileResolverTest : FunSpec({
    test("prefers the canonical Vericore contract over the legacy CodeContext contract") {
        val root = Files.createTempDirectory("vericore-contract-resolver").toFile()
        root.resolve(".vericore-architecture-contract.json").writeText("canonical")
        root.resolve(".codecontext-architecture-contract.json").writeText("legacy")

        val resolution = ArchitectureContractFileResolver.resolve(root)

        resolution.file.name shouldBe ".vericore-architecture-contract.json"
        resolution.usedLegacy shouldBe false
    }

    test("falls back to the legacy CodeContext contract when canonical contract is absent") {
        val root = Files.createTempDirectory("vericore-contract-resolver").toFile()
        root.resolve(".codecontext-architecture-contract.json").writeText("legacy")

        val resolution = ArchitectureContractFileResolver.resolve(root)

        resolution.file.name shouldBe ".codecontext-architecture-contract.json"
        resolution.usedLegacy shouldBe true
    }

    test("resolves the canonical destination when no contract exists") {
        val root = Files.createTempDirectory("vericore-contract-resolver").toFile()

        val resolution = ArchitectureContractFileResolver.resolve(root)

        resolution.file.name shouldBe ".vericore-architecture-contract.json"
        resolution.usedLegacy shouldBe false
    }
})
