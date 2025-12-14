package com.codecontext.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.file.shouldExist
import java.io.File
import kotlin.io.path.createTempDirectory

class StressTest :
        FunSpec({
            test("Stress Test: Analyze 100 files with complex dependencies") {
                val tempDir = createTempDirectory("codecontext-stress").toFile()
                tempDir.deleteOnExit()

                // Generate 100 Kotlin files
                val files =
                        (1..100).map { i ->
                            val name = "Class$i"
                            val file = File(tempDir, "$name.kt")

                            // Generate dependencies: ClassN depends on Class(N-1) and Class(Random)
                            val deps = mutableListOf<String>()
                            if (i > 1) deps.add("Class${i-1}")
                            if (i > 5) deps.add("Class${(1 until i).random()}")

                            val imports = deps.map { "import com.stress.$it" }.joinToString("\n")

                            file.writeText(
                                    """
                package com.stress
                
                $imports
                
                /**
                 * Description for $name.
                 */
                class $name {
                    fun doSomething() { }
                }
            """.trimIndent()
                            )
                            file
                        }

                println("Generated ${files.size} files in ${tempDir.absolutePath}")

                // Execute Analysis via CLI Command logic (but invoking functionality directly to
                // avoid System.exit)
                // actually AnalyzeCommand runs cleanly.

                // We need to bypass Clikt's run check or strictly invoke pipeline manually if
                // needed.
                // But main() is easiest.
                // We'll trust AnalyzeCommand logic.

                // Use a subprocess or just run command logic?
                // Calling main() might call system.exit.
                // Let's call AnalyzeCommand() directly if accessible.
                // We'll mimic the AnalyzeCommand body logic here to test the Core Pipeline
                // integrally.

                val scanner = com.codecontext.core.scanner.RepositoryScanner()
                val scannedFiles = scanner.scan(tempDir.absolutePath)
                assert(scannedFiles.size == 100)

                val parsedFiles =
                        scannedFiles.map {
                            com.codecontext.core.parser.ParserFactory.getParser(it).parse(it)
                        }

                val graph = com.codecontext.core.graph.DependencyGraph()
                graph.build(parsedFiles)
                graph.analyze()

                val pathGen = com.codecontext.core.generator.LearningPathGenerator()
                val path = pathGen.generate(graph)
                assert(path.size == 100)

                val reportFile = File(tempDir, "output/index.html")
                reportFile.parentFile.mkdirs()

                val reporter = com.codecontext.output.ReportGenerator()
                reporter.generate(graph, reportFile.absolutePath, parsedFiles, path)

                reportFile.shouldExist()

                // Cleanup handled by OS mostly, but good practice
                tempDir.deleteRecursively()
            }
        })
