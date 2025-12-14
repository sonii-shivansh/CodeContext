package com.codecontext.core.generator

import com.codecontext.core.graph.RobustDependencyGraph
import org.jgrapht.alg.cycle.CycleDetector
import org.jgrapht.traverse.TopologicalOrderIterator

data class LearningStep(val file: String, val description: String, val reason: String)

class LearningPathGenerator {

    fun generate(graph: RobustDependencyGraph): List<LearningStep> {
        val jGraph = graph.graph

        // 1. Detect and (conceptually) handle cycles
        // JGraphT's TopologicalOrderIterator throws if there are cycles.
        // For MVP, we will try to use it, and fallback if cycle detected.

        val detector = CycleDetector(jGraph)
        if (detector.detectCycles()) {
            println("⚠️ Warning: Cyclic dependencies detected. Learning path might be approximate.")
            // Fallback: Just return nodes sorted by fewest dependencies (clamped out-degree)
            return jGraph.vertexSet().sortedBy { jGraph.outDegreeOf(it) }.map {
                createStep(it, "Cycle Context", "Part of a complex circular dependency.")
            }
        }

        // 2. Topological Sort gives us "Order of Execution" (Dependents LAST).
        // For "Reading Order", we essentially want the same:
        // Read independent files (deps resolved) first.
        // So standard Topological Sort is actually what we want for "Bottom-Up".
        // A -> B (A depends on B). Topo Sort: B, A.
        // Read B first, then A. Perfect.

        val iterator = TopologicalOrderIterator(jGraph)
        val path = mutableListOf<String>()
        iterator.forEachRemaining { path.add(it) }

        // We want Revere Topological Sort (Dependencies first)
        path.reverse()

        // 3. Convert to steps
        return path.map { filePath ->
            val outDegree = jGraph.outDegreeOf(filePath)
            val inDegree = jGraph.inDegreeOf(filePath)

            val category =
                    when {
                        outDegree == 0 -> "Fundamental" // No dependencies
                        inDegree == 0 -> "Entry Point" // Nothing uses this (e.g. Main)
                        else -> "Core Logic" // Intermediate
                    }

            val reason =
                    when (category) {
                        "Fundamental" ->
                                "This file stands alone. Start here to understand basic blocks."
                        "Entry Point" ->
                                "This is a high-level orchestrator. Read this last to see how everything fits."
                        else -> "Connects different parts of the system."
                    }

            createStep(filePath, category, reason)
        }
    }

    private fun createStep(path: String, category: String, reason: String): LearningStep {
        return LearningStep(file = path, description = category, reason = reason)
    }
}
