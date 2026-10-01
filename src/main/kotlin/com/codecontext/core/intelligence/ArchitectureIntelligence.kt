package com.codecontext.core.intelligence

import java.io.File
import kotlinx.serialization.Serializable
import org.jgrapht.Graph
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector
import org.jgrapht.graph.DefaultEdge

@Serializable
data class ArchitectureLayer(
    val name: String,
    val pathPatterns: List<String>,
    val allowedDependencies: List<String> = emptyList()
)

@Serializable
data class ArchitectureRuleConfig(
    val layers: List<ArchitectureLayer> = defaultArchitectureLayers(),
    val forbiddenDependencies: List<String> = emptyList(),
    val enabled: Boolean = true
)

@Serializable
data class ArchitectureFinding(
    val ruleId: String,
    val severity: String,
    val source: String,
    val target: String? = null,
    val relationship: String,
    val evidence: String,
    val category: String = "architecture"
)

@Serializable
data class ArchitectureCycle(
    val members: List<String>
)

@Serializable
data class ArchitectureSummary(
    val filesAnalyzed: Int,
    val dependencyEdges: Int,
    val findings: Int,
    val cycles: Int,
    val crossLayerDependencies: Int,
    val highCouplingFiles: Int
)

@Serializable
data class ArchitectureIntelligenceResult(
    val schemaVersion: String = "1.0",
    val summary: ArchitectureSummary,
    val findings: List<ArchitectureFinding>,
    val cycles: List<ArchitectureCycle>,
    val layers: Map<String, Int>
)

object ArchitectureIntelligenceEngine {
    fun analyze(
        graph: Graph<String, DefaultEdge>,
        ruleConfig: ArchitectureRuleConfig = ArchitectureRuleConfig()
    ): ArchitectureIntelligenceResult {
        if (!ruleConfig.enabled) {
            return ArchitectureIntelligenceResult(
                summary = ArchitectureSummary(graph.vertexSet().size, graph.edgeSet().size, 0, 0, 0, 0),
                findings = emptyList(),
                cycles = emptyList(),
                layers = emptyMap()
            )
        }

        val layerByPath = graph.vertexSet().associateWith { path -> detectLayer(path, ruleConfig.layers) }
        val findings = mutableListOf<ArchitectureFinding>()
        var crossLayerDependencies = 0

        graph.edgeSet().forEach { edge ->
            val source = graph.getEdgeSource(edge)
            val target = graph.getEdgeTarget(edge)
            val sourceLayer = layerByPath[source]
            val targetLayer = layerByPath[target]
            if (sourceLayer != null && targetLayer != null && sourceLayer != targetLayer) {
                crossLayerDependencies++
                val sourceConfig = ruleConfig.layers.firstOrNull { it.name == sourceLayer }
                val allowed = sourceConfig?.allowedDependencies.orEmpty()
                if (targetLayer !in allowed) {
                    findings += ArchitectureFinding(
                        ruleId = "ARCH-LAYER-001",
                        severity = "HIGH",
                        source = relativePath(source),
                        target = relativePath(target),
                        relationship = "dependency",
                        evidence = "$sourceLayer layer depends on $targetLayer layer"
                    )
                }
            }

            val explicitKey = "${sourceLayer ?: "*"}->${targetLayer ?: "*"}"
            if (explicitKey in ruleConfig.forbiddenDependencies) {
                findings += ArchitectureFinding(
                    ruleId = "ARCH-BOUNDARY-001",
                    severity = "HIGH",
                    source = relativePath(source),
                    target = relativePath(target),
                    relationship = "dependency",
                    evidence = "Forbidden architecture boundary: $explicitKey"
                )
            }
        }

        val cycles = KosarajuStrongConnectivityInspector(graph).stronglyConnectedSets()
            .filter { it.size > 1 }
            .map { ArchitectureCycle(it.map(::relativePath).sorted()) }
            .sortedBy { it.members.firstOrNull().orEmpty() }

        cycles.forEach { cycle ->
            findings += ArchitectureFinding(
                ruleId = "ARCH-CYCLE-001",
                severity = "HIGH",
                source = cycle.members.first(),
                relationship = "cycle",
                evidence = "Strongly connected dependency component: ${cycle.members.joinToString(" -> ")}"
            )
        }

        val highCoupling = graph.vertexSet().count { vertex -> graph.inDegreeOf(vertex) + graph.outDegreeOf(vertex) >= 10 }
        graph.vertexSet().filter { graph.inDegreeOf(it) + graph.outDegreeOf(it) >= 10 }
            .sorted()
            .forEach { vertex ->
                findings += ArchitectureFinding(
                    ruleId = "ARCH-COUPLING-001",
                    severity = "MEDIUM",
                    source = relativePath(vertex),
                    relationship = "coupling",
                    evidence = "High structural coupling: ${graph.inDegreeOf(vertex) + graph.outDegreeOf(vertex)} dependency relationships"
                )
            }

        val layerCounts = layerByPath.values.filterNotNull().groupingBy { it }.eachCount().toSortedMap()
        val sortedFindings = findings.distinctBy { listOf(it.ruleId, it.source, it.target, it.relationship, it.evidence) }
            .sortedWith(compareBy<ArchitectureFinding> { it.severityOrder() }.thenBy { it.ruleId }.thenBy { it.source }.thenBy { it.target.orEmpty() })

        return ArchitectureIntelligenceResult(
            summary = ArchitectureSummary(
                filesAnalyzed = graph.vertexSet().size,
                dependencyEdges = graph.edgeSet().size,
                findings = sortedFindings.size,
                cycles = cycles.size,
                crossLayerDependencies = crossLayerDependencies,
                highCouplingFiles = highCoupling
            ),
            findings = sortedFindings,
            cycles = cycles,
            layers = layerCounts
        )
    }

    private fun detectLayer(path: String, layers: List<ArchitectureLayer>): String? {
        val normalized = relativePath(path)
        return layers.firstOrNull { layer -> layer.pathPatterns.any { pattern -> matches(normalized, pattern) } }?.name
    }

    private fun matches(path: String, pattern: String): Boolean {
        val p = pattern.replace('\\', '/').trim('/').lowercase()
        val value = path.lowercase()
        if (p.startsWith("**/")) return value.contains(p.removePrefix("**/"))
        if (p.endsWith("/**")) return value.startsWith(p.removeSuffix("/**").trimEnd('/') + "/")
        return value.contains(p)
    }

    private fun ArchitectureFinding.severityOrder(): Int = when (severity) {
        "CRITICAL" -> 0
        "HIGH" -> 1
        "MEDIUM" -> 2
        "LOW" -> 3
        else -> 4
    }

    private fun relativePath(path: String): String = File(path).path.replace('\\', '/').substringAfterLast("/src/").let {
        if (it == path.replace('\\', '/').substringAfterLast("/src/")) File(path).name else "src/$it"
    }
}

fun defaultArchitectureLayers(): List<ArchitectureLayer> = listOf(
    ArchitectureLayer("api", listOf("/controller/", "/controllers/", "/api/"), listOf("application", "service", "domain")),
    ArchitectureLayer("application", listOf("/service/", "/services/", "/usecase/", "/usecases/"), listOf("domain", "infrastructure", "repository")),
    ArchitectureLayer("domain", listOf("/domain/", "/model/", "/models/"), listOf("domain")),
    ArchitectureLayer("repository", listOf("/repository/", "/repositories/", "/dao/"), listOf("domain", "infrastructure")),
    ArchitectureLayer("infrastructure", listOf("/infrastructure/", "/client/", "/clients/", "/config/"), listOf("domain", "infrastructure"))
)
