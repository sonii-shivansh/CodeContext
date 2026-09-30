package com.codecontext.core.intelligence

import com.codecontext.core.parser.ParsedFile
import kotlinx.serialization.Serializable

/**
 * Stable, machine-readable representation of an analysis run.
 *
 * The snapshot is intentionally independent of HTML rendering and AI providers.
 * It is the contract future CI, SARIF, dashboards and grounded AI features can
 * consume without re-running presentation-specific logic.
 */
@Serializable
data class AnalysisSnapshot(
    val schemaVersion: String = "1.0",
    val repository: RepositorySnapshot,
    val metrics: AnalysisMetrics,
    val files: List<FileSnapshot>,
    val hotspots: List<HotspotSnapshot>,
    val architecture: ArchitectureSnapshot
)

@Serializable
data class RepositorySnapshot(
    val path: String,
    val analyzedAtEpochMillis: Long,
    val languages: List<String>
)

@Serializable
data class AnalysisMetrics(
    val totalFiles: Int,
    val totalNodes: Int,
    val totalEdges: Int,
    val cycleCount: Int,
    val parseFailures: Int = 0
)

@Serializable
data class FileSnapshot(
    val path: String,
    val packageName: String,
    val importCount: Int,
    val churn: Int,
    val authors: List<String>,
    val pageRank: Double,
    val description: String = ""
)

@Serializable
data class HotspotSnapshot(
    val path: String,
    val score: Double,
    val churn: Int,
    val dependents: Int,
    val dependencies: Int
)

@Serializable
data class ArchitectureSnapshot(
    val hasCycles: Boolean,
    val packageCount: Int,
    val crossPackageEdges: Int
)

/** Builds a stable snapshot from the existing analysis primitives. */
object AnalysisSnapshotBuilder {
    fun build(
        repositoryPath: String,
        parsedFiles: List<ParsedFile>,
        graph: org.jgrapht.graph.DefaultDirectedGraph<String, org.jgrapht.graph.DefaultEdge>,
        pageRankScores: Map<String, Double>,
        hasCycles: Boolean,
        parseFailures: Int = 0
    ): AnalysisSnapshot {
        val packageNames = parsedFiles.map { it.packageName }.filter { it.isNotBlank() }.toSet()
        val fileByPath = parsedFiles.associateBy { it.file.absolutePath }

        val files = parsedFiles.map { file ->
            FileSnapshot(
                path = file.file.absolutePath,
                packageName = file.packageName,
                importCount = file.imports.size,
                churn = file.gitMetadata.changeFrequency,
                authors = file.gitMetadata.topAuthors,
                pageRank = pageRankScores[file.file.absolutePath] ?: 0.0,
                description = file.description
            )
        }

        val hotspots = pageRankScores.entries
            .sortedByDescending { it.value }
            .take(20)
            .map { (path, score) ->
                HotspotSnapshot(
                    path = path,
                    score = score,
                    churn = fileByPath[path]?.gitMetadata?.changeFrequency ?: 0,
                    dependents = if (graph.containsVertex(path)) graph.inDegreeOf(path) else 0,
                    dependencies = if (graph.containsVertex(path)) graph.outDegreeOf(path) else 0
                )
            }

        var crossPackageEdges = 0
        graph.edgeSet().forEach { edge ->
            val source = graph.getEdgeSource(edge)
            val target = graph.getEdgeTarget(edge)
            val sourcePackage = fileByPath[source]?.packageName
            val targetPackage = fileByPath[target]?.packageName
            if (!sourcePackage.isNullOrBlank() && !targetPackage.isNullOrBlank() && sourcePackage != targetPackage) {
                crossPackageEdges++
            }
        }

        val languages = parsedFiles.mapNotNull { file ->
            when (file.file.extension.lowercase()) {
                "kt", "kts" -> "Kotlin"
                "java" -> "Java"
                else -> null
            }
        }.distinct().sorted()

        return AnalysisSnapshot(
            repository = RepositorySnapshot(
                path = repositoryPath,
                analyzedAtEpochMillis = System.currentTimeMillis(),
                languages = languages
            ),
            metrics = AnalysisMetrics(
                totalFiles = parsedFiles.size,
                totalNodes = graph.vertexSet().size,
                totalEdges = graph.edgeSet().size,
                cycleCount = if (hasCycles) 1 else 0,
                parseFailures = parseFailures
            ),
            files = files,
            hotspots = hotspots,
            architecture = ArchitectureSnapshot(
                hasCycles = hasCycles,
                packageCount = packageNames.size,
                crossPackageEdges = crossPackageEdges
            )
        )
    }
}
