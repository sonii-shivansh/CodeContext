package com.codecontext.core

import com.codecontext.core.generator.LearningPathGenerator
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.intelligence.ANALYSIS_SCHEMA_VERSION
import com.codecontext.core.intelligence.AnalysisMetrics
import com.codecontext.core.intelligence.AnalysisSnapshot
import com.codecontext.core.intelligence.ArchitectureSnapshot
import com.codecontext.core.intelligence.EngineeringRiskEngine
import com.codecontext.core.intelligence.FileSnapshot
import com.codecontext.core.intelligence.HotspotSnapshot
import com.codecontext.core.intelligence.RepositorySnapshot
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

            "EngineeringRiskEngine should be deterministic for identical snapshots" {
                val snapshot = AnalysisSnapshot(
                        schemaVersion = ANALYSIS_SCHEMA_VERSION,
                        repository = RepositorySnapshot("/repo", 1L, listOf("Kotlin")),
                        metrics = AnalysisMetrics(2, 2, 1, false),
                        files = listOf(
                                FileSnapshot("/repo/A.kt", "a", 20, 12, listOf("dev"), 0.03, 11, 2),
                                FileSnapshot("/repo/B.kt", "b", 2, 0, listOf("dev"), 0.001, 0, 0)
                        ),
                        hotspots = listOf(HotspotSnapshot("/repo/A.kt", 0.03, 12, 11, 2)),
                        architecture = ArchitectureSnapshot(false, 2, 1)
                )

                val first = EngineeringRiskEngine.calculate(snapshot)
                val second = EngineeringRiskEngine.calculate(snapshot)

                first shouldBe second
                first.first().path shouldBe "/repo/A.kt"

                val tied = snapshot.copy(
                        files = listOf(
                                FileSnapshot("/repo/C.kt", "c", 2, 0, listOf("dev"), 0.001, 0, 0),
                                FileSnapshot("/repo/B.kt", "b", 2, 0, listOf("dev"), 0.001, 0, 0)
                        )
                )
                EngineeringRiskEngine.calculate(tied).map { it.path } shouldBe
                        listOf("/repo/B.kt", "/repo/C.kt")
            }
        })
