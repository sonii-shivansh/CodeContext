package com.codecontext.output

import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.parser.ParsedFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotContain
import java.io.File
import java.nio.file.Files

class ReportGeneratorTest : StringSpec({
    "generated reports should be self-contained and not load visualization code from a CDN" {
        val tempDir = Files.createTempDirectory("codecontext-report-test").toFile()
        try {
            val parsedFiles = listOf(
                ParsedFile(File(tempDir, "A.kt"), "example", emptyList(), "A"),
                ParsedFile(File(tempDir, "B.kt"), "example", emptyList(), "B")
            )
            val graph = RobustDependencyGraph()
            graph.build(parsedFiles)
            graph.analyze()

            val report = File(tempDir, "index.html")
            ReportGenerator().generate(graph, report.absolutePath, parsedFiles, emptyList())
            val html = report.readText()

            html shouldNotContain "unpkg.com"
            html shouldNotContain "https://"
            html.contains("CodeContext Analysis Report") shouldBe true
        } finally {
            tempDir.deleteRecursively()
        }
    }
})
