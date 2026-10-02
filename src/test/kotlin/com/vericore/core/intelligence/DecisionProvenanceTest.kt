package com.vericore.core.intelligence

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import java.nio.file.Files

class DecisionProvenanceTest : FunSpec({
    test("same inputs produce the same stable provenance id") {
        val first = DecisionProvenance.create("prepare", "abc123", "1.0", listOf("repo.metrics", "hotspot.1"))
        val second = DecisionProvenance.create("prepare", "abc123", "1.0", listOf("hotspot.1", "repo.metrics"))

        first.id shouldBe second.id
        first.id shouldHaveLength 64
    }

    test("unknown git state remains explicit") {
        val provenance = DecisionProvenance.create("verify", null, "1.0", emptyList())

        provenance.repositoryCommit shouldBe null
        provenance.unknownReason shouldBe "repository commit unavailable"
    }

    test("captures HEAD commit from a git repository") {
        val repo = Files.createTempDirectory("vericore-provenance").toFile()
        org.eclipse.jgit.api.Git.init().setDirectory(repo).call().use { git ->
            repo.resolve("README.md").writeText("fixture")
            git.add().addFilepattern("README.md").call()
            git.commit().setMessage("fixture").setAuthor("Test", "test@example.com").call()

            val provenance = DecisionProvenance.capture(repo.path, "prepare", "1.0", listOf("repo.metrics"))
            provenance.repositoryCommit shouldBe git.repository.resolve("HEAD").name
            provenance.unknownReason shouldBe null
        }
    }
})
