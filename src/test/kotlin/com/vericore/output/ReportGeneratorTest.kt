package com.vericore.output

import com.vericore.core.graph.RobustDependencyGraph
import com.vericore.core.parser.ParsedFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files

class ReportGeneratorTest : StringSpec({
    "generated reports should be self-contained and not load visualization code from a CDN" {
        val tempDir = Files.createTempDirectory("codecontext-report-test").toFile()
        try {
            val parsedFiles = listOf(
                ParsedFile(File(tempDir, "A.kt"), "example", emptyList()),
                ParsedFile(File(tempDir, "B.kt"), "example", emptyList())
            )
            val graph = RobustDependencyGraph()
            graph.build(parsedFiles)
            graph.analyze()

            val report = File(tempDir, "index.html")
            ReportGenerator().generate(graph, report.absolutePath, parsedFiles, emptyList())
            val html = report.readText()

            ("unpkg.com" in html) shouldBe false
            ("https://" in html) shouldBe false
            html.contains("CodeContext Analysis Report") shouldBe true
        } finally {
            tempDir.deleteRecursively()
        }
    }
})
