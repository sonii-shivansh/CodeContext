package com.vericore.core.temporal

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import org.eclipse.jgit.api.Git

class TemporalAnalyzerTest : StringSpec({
    "temporal snapshot reads committed source lines without checking out history" {
        val root = Files.createTempDirectory("codecontext-temporal-test").toFile()
        root.deleteOnExit()
        val source = root.resolve("Example.java")
        source.writeText("class Example {\n    void run() {}\n}\n")

        Git.init().setDirectory(root).call().use { git ->
            git.repository.config.apply {
                setString("user", null, "name", "CodeContext Test")
                setString("user", null, "email", "codecontext@example.invalid")
                save()
            }
            git.add().addFilepattern("Example.java").call()
            git.commit().setMessage("initial source").call()
        }

        source.appendText("uncommitted line\n")

        val snapshots = TemporalAnalyzer(root.path).analyzeEvolution(monthsBack = 0, intervalDays = 1)

        snapshots.size shouldBe 1
        snapshots.single().totalFiles shouldBe 1
        snapshots.single().totalLines shouldBe 3
        snapshots.single().topHotspots shouldBe listOf("Example.java")
    }
})
