package com.codecontext.core.graph

import com.codecontext.core.parser.ParsedFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

class DependencyGraphTest :
        FunSpec({
            test("Graph should link simple imports") {
                val fileA = File("src/com/A.kt")
                val fileB = File("src/com/B.kt")

                val parsedA = ParsedFile(fileA, "com", listOf("com.B"))
                val parsedB = ParsedFile(fileB, "com", emptyList())

                val graph = RobustDependencyGraph()
                graph.build(listOf(parsedA, parsedB))

                graph.graph.containsEdge(fileA.absolutePath, fileB.absolutePath) shouldBe true
            }

            test("Graph should handle wildcard imports") {
                val fileA = File("src/com/A.kt")
                val fileB = File("src/utils/B.kt")
                val fileC = File("src/utils/C.kt")

                val parsedA = ParsedFile(fileA, "com", listOf("utils.*"))
                val parsedB = ParsedFile(fileB, "utils", emptyList())
                val parsedC = ParsedFile(fileC, "utils", emptyList())

                val graph = RobustDependencyGraph()
                graph.build(listOf(parsedA, parsedB, parsedC))

                graph.graph.containsEdge(fileA.absolutePath, fileB.absolutePath) shouldBe true
                graph.graph.containsEdge(fileA.absolutePath, fileC.absolutePath) shouldBe true
            }
        })
