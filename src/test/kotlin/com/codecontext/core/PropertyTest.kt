package com.codecontext.core

import com.codecontext.core.generator.LearningPathGenerator
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.parser.ParsedFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.io.File

class PropertyTest :
        StringSpec({
            "LearningPathGenerator should always include all files in the output" {
                checkAll(1000, Arb.list(Arb.stringPattern("[a-zA-Z0-9]{3,10}"), 1..20)) { fileNames
                    ->
                    // Create unique files
                    val uniqueFiles = fileNames.distinct()
                    if (uniqueFiles.isNotEmpty()) {
                        val parsedFiles =
                                uniqueFiles.map { name ->
                                    ParsedFile(
                                            file = File(name),
                                            packageName = "com.test",
                                            imports =
                                                    emptyList(), // Random deps handled below maybe?
                                            description = "Simulated $name"
                                    )
                                }

                        // Randomly assign dependencies within the set
                        // Effectively building a random graph
                        // But simplified: here files have 0 deps.

                        val graph = RobustDependencyGraph()
                        graph.build(parsedFiles)

                        val generator = LearningPathGenerator()
                        val path = generator.generate(graph)

                        path.map { File(it.file).name } shouldContainExactlyInAnyOrder uniqueFiles
                    }
                }
            }

            "LearningPathGenerator should handle random dependency trees without crashing" {
                // Generator for a list of ParsedFiles with random dependencies pointing to each
                // other
                val fileGen = Arb.list(Arb.stringPattern("[a-z]{5}"), 5..20)

                checkAll(1000, fileGen) { names ->
                    val uniqueNames = names.distinct()
                    val parsedFiles =
                            uniqueNames.map { name ->
                                // Randomly pick dependencies from the other names
                                val deps =
                                        uniqueNames
                                                .filter { it != name }
                                                .shuffled()
                                                .take((0..3).random())
                                ParsedFile(
                                        file = File("$name.kt"),
                                        packageName = "com.pkg",
                                        imports = deps, // Here we simulate imports as strings
                                        // matching names
                                        description = "Random"
                                )
                            }

                    val graph = RobustDependencyGraph()
                    graph.build(parsedFiles)

                    val generator = LearningPathGenerator()
                    // effectively verifying it doesn't throw Exception (StackOverflow, etc)
                    val path = generator.generate(graph)

                    path.size shouldBe uniqueNames.size
                }
            }
        })
