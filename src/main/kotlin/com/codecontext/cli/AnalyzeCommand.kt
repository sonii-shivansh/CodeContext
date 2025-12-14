package com.codecontext.cli

import com.codecontext.core.graph.DependencyGraph
import com.codecontext.core.parser.ParserFactory
import com.codecontext.core.scanner.RepositoryScanner
import com.codecontext.output.ReportGenerator
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import java.io.File
import kotlin.system.measureTimeMillis

class AnalyzeCommand :
        CliktCommand(name = "analyze", help = "Analyze a codebase and generate a report") {
    private val path by argument("path", help = "Path to the repository to analyze").default(".")

    override fun run() {
        echo("🚀 Starting CodeContext analysis for: $path")

        val time = measureTimeMillis {
            // 1. Scan
            echo("📂 Scanning repository...")
            val scanner = RepositoryScanner()
            val files = scanner.scan(path)
            echo("   Found ${files.size} Java/Kotlin files.")

            if (files.isEmpty()) {
                echo("❌ No source files found to analyze.")
                return
            }

            // 2. Parse
            echo("🧠 Parsing code...")
            val parsedFilesStart = files.map { file -> ParserFactory.getParser(file).parse(file) }

            // 2.5 Smart Context (Git Analysis)
            echo("📜 Analyzing Git history (Smart Context)...")
            val gitAnalyzer = com.codecontext.core.scanner.GitAnalyzer()
            val parsedFiles = gitAnalyzer.analyze(File(path).absolutePath, parsedFilesStart)

            // 3. Build Graph
            echo("🕸️ Building dependency graph...")
            val graph = DependencyGraph()
            graph.build(parsedFiles)
            graph.analyze()

            val hotspots = graph.getTopHotspots(5)
            echo("🗺️ Your Codebase Map")
            echo("├─ 🔥 Hot Zones (Critical Files):")
            hotspots.forEachIndexed { index, (file, score) ->
                val prefix = if (index == hotspots.lastIndex) "│   └─" else "│   ├─"
                echo("$prefix ${File(file).name} (Score: ${String.format("%.4f", score)})")
            }

            // 3.5 Learning Path
            echo("🎓 Generating Learning Path...")
            val pathGenerator = com.codecontext.core.generator.LearningPathGenerator()
            val learningPath = pathGenerator.generate(graph)

            echo("├─ 🎯 Recommended Start (Entry Points):")
            learningPath.take(5).forEachIndexed { index, step ->
                val prefix =
                        if (index == 4 || index == learningPath.lastIndex) "    └─" else "    ├─"
                echo("$prefix ${File(step.file).name} [${step.description}]")
            }

            // 4. Report
            echo("📊 Generating report...")
            val outputDir = File("output")
            if (!outputDir.exists()) outputDir.mkdirs()
            val reportFile = File(outputDir, "index.html")

            val generator = ReportGenerator()
            generator.generate(graph, reportFile.absolutePath, parsedFiles, learningPath)

            echo("✅ Report generated at: ${reportFile.absolutePath}")
        }

        echo("✨ Analysis complete in ${time}ms")
    }
}
