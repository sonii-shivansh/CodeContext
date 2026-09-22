package com.codecontext.core

import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.parser.ParserFactory
import com.codecontext.core.scanner.RepositoryScanner
import com.codecontext.output.ReportGenerator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.random.Random

class StressTest : FunSpec({
    test("analyzes 1000 files with deterministic dependencies") {
        val tempDir = createTempDirectory("codecontext-stress").toFile()
        tempDir.deleteOnExit()
        val random = Random(42)
        (1..1000).forEach { i ->
            val name = "Class$i"
            val file = File(tempDir, "$name.kt")
            val deps = buildList {
                if (i > 1) add("Class${i - 1}")
                if (i > 5) add("Class${random.nextInt(1, i)}")
            }
            val imports = deps.joinToString("\n") { "import com.stress.$it" }
            file.writeText("""
                package com.stress
                $imports
                class $name { fun doSomething() {} }
            """.trimIndent())
        }
        val scannedFiles = RepositoryScanner().scan(tempDir.absolutePath)
        scannedFiles.size shouldBe 1000
        val parsedFiles = scannedFiles.map { ParserFactory.getParser(it).parse(it) }
        val graph = RobustDependencyGraph()
        graph.build(parsedFiles)
        graph.analyze()
        val path = com.codecontext.core.generator.LearningPathGenerator().generate(graph)
        path.size shouldBe 1000
        val reportFile = File(tempDir, "output/index.html").apply { parentFile.mkdirs() }
        ReportGenerator().generate(graph, reportFile.absolutePath, parsedFiles, path)
        reportFile.shouldExist()
        tempDir.deleteRecursively()
    }
})
