package com.codecontext.verification

import com.codecontext.cli.CodeParallelParser
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.scanner.RepositoryScanner
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackendVerificationTest {

    @Test
    fun `verify backend logic on self`() {
        val rootDir = File(".").absoluteFile
        // 1. Scan
        println("Scanning $rootDir...")
        val scanner = RepositoryScanner()
        val files = scanner.scan(rootDir.absolutePath)

        // Assert we found some files
        assertTrue(files.isNotEmpty(), "Should find files in the project")
        val mainKt = files.find { it.name == "Main.kt" }
        assertTrue(mainKt != null, "Should find Main.kt")

        // 2. Parse
        println("Parsing ${files.size} files...")
        val parser = CodeParallelParser()
        val parsedFiles = runBlocking { parser.parseFiles(files) }

        assertEquals(files.size, parsedFiles.size, "Should parse all found files")

        val parsedMain = parsedFiles.find { it.file.name == "ImprovedAnalyzeCommand.kt" }
        assertTrue(parsedMain != null, "Should have parsed ImprovedAnalyzeCommand.kt")

        // Verify imports are extracted (using Regex parser for Kotlin)
        // ImprovedAnalyzeCommand imports com.codecontext.core.graph.RobustDependencyGraph
        val hasGraphImport =
                parsedMain?.imports?.any { it.contains("RobustDependencyGraph") } == true
        assertTrue(hasGraphImport, "ImprovedAnalyzeCommand should import RobustDependencyGraph")

        // 3. Build Graph
        println("Building graph...")
        val graphBuilder = RobustDependencyGraph()
        val buildResult = graphBuilder.build(parsedFiles)
        assertTrue(buildResult.isSuccess, "Graph build should succeed")

        val graph = graphBuilder.graph
        println("Graph has ${graph.vertexSet().size} vertices and ${graph.edgeSet().size} edges")

        assertTrue(graph.vertexSet().isNotEmpty(), "Graph should not be empty")

        // Verify specific edge: ImprovedAnalyzeCommand -> RobustDependencyGraph
        // The graph uses absolute paths as vertices
        val sourceFile = parsedMain!!.file.absolutePath
        val targetParsed = parsedFiles.find { it.file.name == "RobustDependencyGraph.kt" }
        assertTrue(targetParsed != null, "Should have parsed RobustDependencyGraph.kt")

        val targetFile = targetParsed!!.file.absolutePath

        println("Target File: $targetFile")
        println("Target Package: ${targetParsed.packageName}")
        println(
                "Target FQCN should be: ${targetParsed.packageName}.${targetParsed.file.nameWithoutExtension}"
        )

        // Check if edge exists
        val hasEdge = graph.containsEdge(sourceFile, targetFile)

        if (!hasEdge) {
            println("Edge missing! Detailed Debug:")
            println("Source Path: $sourceFile")
            println("Target Path: $targetFile")

            val outgoing = graph.outgoingEdgesOf(sourceFile)
            println("Outgoing edges from Source (${outgoing.size}):")
            outgoing.forEach { edge ->
                val target = graph.getEdgeTarget(edge)
                println(" -> $target")
                if (target.equals(targetFile, ignoreCase = true)) {
                    println("    (Match with ignoreCase! Case mismatch problem?)")
                }
            }

            // Debug imports again
            println("Imports of ImprovedAnalyzeCommand:")
            parsedMain.imports.forEach { println("  - $it") }

            val expectedFqcn = "com.codecontext.core.graph.RobustDependencyGraph"
            println("Expected FQCN: $expectedFqcn")
            println("Imports contain it? ${parsedMain.imports.contains(expectedFqcn)}")
        }

        assertTrue(hasEdge, "Should have edge from ImprovedAnalyzeCommand to RobustDependencyGraph")

        // 4. Analyze Hotspots
        val analyzeResult = graphBuilder.analyze()
        assertTrue(analyzeResult.isSuccess, "Graph analysis should succeed")

        val hotspots = graphBuilder.getTopHotspots(5)
        println("Top Hotspots:")
        hotspots.forEach { (path, score) -> println("${File(path).name}: $score") }

        assertTrue(hotspots.isNotEmpty(), "Should have hotspots")
        // RobustDependencyGraph is likely a hotspot or referenced by commands
        // or ParsedFile might be a hotspot?
    }
}
