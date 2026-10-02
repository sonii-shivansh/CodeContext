package com.codecontext.core.reality

import com.codecontext.core.intelligence.AnalysisSnapshot
import com.codecontext.core.intelligence.EngineeringContextSnapshot
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
        require(analysis.repository.languages.sorted() == context.languages.sorted()) {
            "Analysis/context language sets differ; generate both artifacts from the same repository state."
        }

        val analysisDigest = sha256(json.encodeToString(AnalysisSnapshot.serializer(), analysis))
        val contextDigest = sha256(
            json.encodeToString(EngineeringContextSnapshot.serializer(), context)
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
