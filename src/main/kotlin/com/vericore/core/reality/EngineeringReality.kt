package com.vericore.core.reality

import com.vericore.core.intelligence.AnalysisSnapshot
import com.vericore.core.intelligence.EngineeringContextSnapshot
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val ENGINEERING_REALITY_SCHEMA_VERSION = "1.0"

/**
 * A deterministic, compact description of the repository's current engineering reality.
 *
 * This composition layer binds existing deterministic artifacts together so downstream
 * tools and AI agents can reason about one explicit repository state without guessing
 * which artifacts belong together.
 */
@Serializable
data class EngineeringRealitySnapshot(
    val schemaVersion: String,
    val repositoryCommit: String?,
    val analysisSchemaVersion: String,
    val contextSchemaVersion: String,
    val languages: List<String>,
    val fileCount: Int,
    val graphNodes: Int,
    val graphEdges: Int,
    val parseFailures: Int,
    val hasCycles: Boolean,
    val packageCount: Int,
    val crossPackageEdges: Int,
    val hotspotCount: Int,
    val dirty: Boolean,
    val changedPaths: List<String>,
    val analysisDigest: String,
    val contextDigest: String,
    val realityDigest: String
)

object EngineeringRealityEngine {
    private val json = Json { encodeDefaults = true; prettyPrint = true }

    fun build(
        analysis: AnalysisSnapshot,
        context: EngineeringContextSnapshot
    ): EngineeringRealitySnapshot {
        // Provenance/state identity is the first boundary: if the analysis is stale,
        // report that directly before evaluating secondary semantic compatibility.
        require(
            analysis.repository.repositoryCommit == null ||
                context.repositoryCommit == null ||
                analysis.repository.repositoryCommit == context.repositoryCommit
        ) {
            "Analysis snapshot is stale: its Git commit does not match the current repository state. Run 'vericore analyze' again."
        }

        require(
            analysis.repository.repositoryStateDigest == null ||
                analysis.repository.repositoryStateDigest == context.snapshotDigest
        ) {
            "Analysis snapshot is stale: its source-state digest does not match the current repository state. Run 'vericore analyze' again."
        }

        require(analysis.repository.languages.sorted() == context.languages.sorted()) {
            "Analysis/context language sets differ; generate both artifacts from the same repository state."
        }

        // analyzedAtEpochMillis is deliberately excluded from the identity. Re-running
        // analysis against unchanged repository state must not create a new reality digest.
        val analysisDigest = sha256(
            buildString {
                append(analysis.schemaVersion).append('|')
                append(analysis.repository.languages.sorted()).append('|')
                append(analysis.metrics).append('|')
                analysis.files.sortedBy { it.path }.forEach { append(it).append('|') }
                analysis.hotspots.sortedBy { it.path }.forEach { append(it).append('|') }
                append(analysis.architecture)
            }
        )
        val contextDigest = sha256(
            buildString {
                append(context.schemaVersion).append('|')
                append(context.repositoryCommit.orEmpty()).append('|')
                context.files.sortedBy { it.path }.forEach { append(it).append('|') }
                append(context.languages.sorted()).append('|')
                append(context.dirty).append('|')
                append(context.changedPaths.sorted()).append('|')
                append(context.snapshotDigest)
            }
        )
        val identity = listOf(
            ENGINEERING_REALITY_SCHEMA_VERSION,
            context.repositoryCommit.orEmpty(),
            analysisDigest,
            contextDigest
        ).joinToString("|")

        return EngineeringRealitySnapshot(
            schemaVersion = ENGINEERING_REALITY_SCHEMA_VERSION,
            repositoryCommit = context.repositoryCommit,
            analysisSchemaVersion = analysis.schemaVersion,
            contextSchemaVersion = context.schemaVersion,
            languages = analysis.repository.languages.sorted(),
            fileCount = analysis.metrics.totalFiles,
            graphNodes = analysis.metrics.totalNodes,
            graphEdges = analysis.metrics.totalEdges,
            parseFailures = analysis.metrics.parseFailures,
            hasCycles = analysis.architecture.hasCycles,
            packageCount = analysis.architecture.packageCount,
            crossPackageEdges = analysis.architecture.crossPackageEdges,
            hotspotCount = analysis.hotspots.size,
            dirty = context.dirty,
            changedPaths = context.changedPaths.sorted(),
            analysisDigest = analysisDigest,
            contextDigest = contextDigest,
            realityDigest = sha256(identity)
        )
    }

    fun encode(snapshot: EngineeringRealitySnapshot): String =
        json.encodeToString(EngineeringRealitySnapshot.serializer(), snapshot)

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
