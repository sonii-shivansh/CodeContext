package com.codecontext.core.generator

import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.parser.ParsedFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

class LearningPathGeneratorTest :
        FunSpec({
            test("Should order simple dependency chain C -> B -> A") {
                // A depends on B, B depends on C
                // Reading order: C, B, A
                val fileA = ParsedFile(File("A.kt"), "", listOf("B"))
                val fileB = ParsedFile(File("B.kt"), "", listOf("C"))
                val fileC = ParsedFile(File("C.kt"), "", emptyList())

                val graph = RobustDependencyGraph()
                graph.build(listOf(fileA, fileB, fileC))

                val generator = LearningPathGenerator()
                val path = generator.generate(graph)

                path.map { it.file } shouldBe
                        listOf(
                                File("C.kt").absolutePath,
                                File("B.kt").absolutePath,
                                File("A.kt").absolutePath
                        )
            }

            test("Should order tree structure correctly") {
                // Main -> [Utils, Core]
                // Utils -> []
                // Core -> [Utils]
                // Order: Utils, Core, Main
                val main = ParsedFile(File("Main.kt"), "", listOf("Core", "Utils"))
                val core = ParsedFile(File("Core.kt"), "", listOf("Utils"))
                val utils = ParsedFile(File("Utils.kt"), "", emptyList())

                val graph = RobustDependencyGraph()
                graph.build(listOf(main, core, utils))

                val generator = LearningPathGenerator()
                val result = generator.generate(graph).map { it.file }

                // Utils must be first
                result.first() shouldBe File("Utils.kt").absolutePath
                // Main must be last
                result.last() shouldBe File("Main.kt").absolutePath
            }

            test("Should handle cycles gracefully") {
                // A -> B -> A
                val fileA = ParsedFile(File("A.kt"), "", listOf("B"))
                val fileB = ParsedFile(File("B.kt"), "", listOf("A"))

                val graph = RobustDependencyGraph()
                graph.build(listOf(fileA, fileB))

                val generator = LearningPathGenerator()
                val path = generator.generate(graph)

                // Should return a list containing both, order might vary but shouldn't crash
                path.size shouldBe 2
                path.any { it.description.contains("Cycle") } shouldBe true
            }
        })
