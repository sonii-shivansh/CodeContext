package com.codecontext.core.graph

import com.codecontext.core.parser.ParsedFile
import org.jgrapht.alg.cycle.CycleDetector
import org.jgrapht.alg.scoring.PageRank
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

class RobustDependencyGraph {
    val graph = DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge::class.java)
    val pageRankScores = mutableMapOf<String, Double>()
    var hasCycles = false

    fun build(parsedFiles: List<ParsedFile>): Result<Unit> {
        return try {
            // Build class map
            val classMap = mutableMapOf<String, String>()

            parsedFiles.forEach { parsed ->
                val filePath = parsed.file.absolutePath
                graph.addVertex(filePath)

                val className = parsed.file.nameWithoutExtension
                val fqcn =
                        if (parsed.packageName.isNotEmpty()) "${parsed.packageName}.$className"
                        else className
                classMap[fqcn] = filePath
            }

            // Add edges with safety checks
            parsedFiles.forEach { source ->
                source.imports.forEach { import ->
                    if (import.endsWith(".*")) {
                        val packageName = import.removeSuffix(".*")
                        parsedFiles.filter { it.packageName == packageName }.forEach { target ->
                            addEdgeSafely(source.file.absolutePath, target.file.absolutePath)
                        }
                    } else {
                        // Standard Import
                        classMap[import]?.let { targetPath ->
                            addEdgeSafely(source.file.absolutePath, targetPath)
                        }
                    }
                }
            }

            // Detect cycles
            if (graph.vertexSet().isNotEmpty()) {
                val cycleDetector = CycleDetector(graph)
                hasCycles = cycleDetector.detectCycles()

                if (hasCycles) {
                    println(
                            "⚠️ Warning: Circular dependencies detected (cycles found involving ${cycleDetector.findCycles().size} vertices)"
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addEdgeSafely(source: String, target: String) {
        if (source != target && !graph.containsEdge(source, target)) {
            try {
                if (graph.containsVertex(source) && graph.containsVertex(target)) {
                    graph.addEdge(source, target)
                }
            } catch (e: Exception) {
                // Edge already exists or would create self-loop (should be caught by if check but
                // just in case)
            }
        }
    }

    fun analyze(): Result<Unit> {
        return try {
            if (graph.vertexSet().isEmpty()) {
                // Not necessarily an error, just empty repo
                return Result.success(Unit)
            }

            // PageRank handles cycles gracefully by damping
            val pageRank = PageRank(graph, 0.85, 100) // damping=0.85, maxIterations=100

            graph.vertexSet().forEach { vertex ->
                pageRankScores[vertex] = pageRank.getVertexScore(vertex)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTopHotspots(limit: Int = 10): List<Pair<String, Double>> {
        return pageRankScores.entries.sortedByDescending { it.value }.take(limit).map {
            it.key to it.value
        }
    }
}
