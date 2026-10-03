package com.vericore.core.temporal

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.time.Instant
import java.util.Date
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent

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

    "evolution sampling does not select commits outside the requested history window" {
        val root = Files.createTempDirectory("codecontext-temporal-window-test").toFile()
        root.deleteOnExit()
        val source = root.resolve("Example.java")
        source.writeText("class Example {}\n")

        Git.init().setDirectory(root).call().use { git ->
            val config = git.repository.config
            config.setString("user", null, "name", "CodeContext Test")
            config.setString("user", null, "email", "codecontext@example.invalid")
            config.save()

            git.add().addFilepattern("Example.java").call()
            val now = Instant.now()
            val oldDate = Date.from(now.minusSeconds(50L * 24L * 60L * 60L))
            git.commit()
                .setMessage("old source")
                .setAuthor(PersonIdent("CodeContext Test", "codecontext@example.invalid", oldDate, java.util.TimeZone.getTimeZone("UTC")))
                .setCommitter(PersonIdent("CodeContext Test", "codecontext@example.invalid", oldDate, java.util.TimeZone.getTimeZone("UTC")))
                .call()

            source.appendText("current source\n")
            git.add().addFilepattern("Example.java").call()
            val headDate = Date.from(now)
            val head = git.commit()
                .setMessage("current source")
                .setAuthor(PersonIdent("CodeContext Test", "codecontext@example.invalid", headDate, java.util.TimeZone.getTimeZone("UTC")))
                .setCommitter(PersonIdent("CodeContext Test", "codecontext@example.invalid", headDate, java.util.TimeZone.getTimeZone("UTC")))
                .call()

            val snapshots = TemporalAnalyzer(root.path).analyzeEvolution(monthsBack = 1, intervalDays = 30)

            snapshots.map { it.commitHash } shouldBe listOf(head.name)
        }
    }
})
