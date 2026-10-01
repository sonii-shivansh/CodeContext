package com.codecontext.core.intelligence

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

const val PR_INTELLIGENCE_SCHEMA_VERSION = "1.0"

@Serializable
enum class ChangeType { ADDED, MODIFIED, DELETED, RENAMED, COPIED }

@Serializable
data class ChangedFile(
    val path: String,
    val changeType: ChangeType,
    val oldPath: String? = null,
    val additions: Int = 0,
    val deletions: Int = 0
)

@Serializable
data class ChangeSet(
    val files: List<ChangedFile>,
    val source: String = "local"
) {
    val addedLines: Int get() = files.sumOf { it.additions }
    val deletedLines: Int get() = files.sumOf { it.deletions }
}

@Serializable
enum class FindingSeverity { INFO, LOW, MEDIUM, HIGH, CRITICAL }

@Serializable
data class PRFinding(
    val ruleId: String,
    val severity: FindingSeverity,
    val paths: List<String>,
    val evidence: Map<String, String>,
    val reason: String
)

@Serializable
data class ChangeSummary(
    val filesChanged: Int,
    val filesAdded: Int,
    val filesModified: Int,
    val filesDeleted: Int,
    val filesRenamed: Int,
    val filesCopied: Int,
    val additions: Int,
    val deletions: Int
)

@Serializable
data class PRIntelligenceResult(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val schemaVersion: String = PR_INTELLIGENCE_SCHEMA_VERSION,
    val changeSummary: ChangeSummary,
    val impactedFiles: Int,
    val impactedPackages: Int,
    val crossPackageImpacts: Int,
    val testCandidates: List<String>,
    val findings: List<PRFinding>,
    val aggregateSeverity: FindingSeverity
)

object PRIntelligenceEngine {
    fun analyze(
        changeSet: ChangeSet,
        impact: ChangeImpactResult,
        risks: List<EngineeringRisk> = emptyList(),
        packageByPath: Map<String, String> = emptyMap(),
        testCandidates: Collection<String> = emptyList()
    ): PRIntelligenceResult {
        val changed = changeSet.files.map { normalize(it.path) }.toSet()
        val unresolved = changeSet.files.filter { it.changeType == ChangeType.DELETED || normalize(it.path) !in impact.changedPaths }
        val highRiskChanged = risks.filter { normalize(it.path) in changed && it.level in setOf(RiskLevel.HIGH, RiskLevel.CRITICAL) }
        val candidateTests = testCandidates.map(::normalize).distinct().sorted()
        val findings = mutableListOf<PRFinding>()

        if (unresolved.isNotEmpty()) {
            findings += PRFinding(
                ruleId = "CHANGE_UNRESOLVED",
                severity = FindingSeverity.MEDIUM,
                paths = unresolved.map { normalize(it.path) }.sorted(),
                evidence = mapOf("count" to unresolved.size.toString()),
                reason = "One or more changed files could not be resolved in the analyzed source graph; static impact coverage is incomplete for those files."
            )
        }

        if (impact.summary.maxDepth >= 3 || impact.summary.impactedFiles >= 10) {
            findings += PRFinding(
                ruleId = "IMPACT_BROAD",
                severity = if (impact.summary.impactedFiles >= 25 || impact.summary.maxDepth >= 5) FindingSeverity.HIGH else FindingSeverity.MEDIUM,
                paths = impact.nodes.filter { it.relationship != ImpactRelationship.CHANGED }.map { normalize(it.path) }.sorted(),
                evidence = mapOf("impactedFiles" to impact.summary.impactedFiles.toString(), "maxDepth" to impact.summary.maxDepth.toString()),
                reason = "The change reaches a substantial dependency surface; affected dependents should be reviewed and tested."
            )
        }

        if (impact.summary.crossPackageImpacts > 0) {
            val changedPackages = changed.mapNotNull { packageByPath[it] }.filter(String::isNotBlank).toSet()
            val impactedPaths = impact.nodes.filter { it.relationship != ImpactRelationship.CHANGED }.map { normalize(it.path) }
            findings += PRFinding(
                ruleId = "ARCH_CROSS_PACKAGE",
                severity = if (impact.summary.crossPackageImpacts >= 10) FindingSeverity.HIGH else FindingSeverity.MEDIUM,
                paths = impactedPaths.sorted(),
                evidence = mapOf("changedPackages" to changedPackages.size.toString(), "crossPackageImpacts" to impact.summary.crossPackageImpacts.toString()),
                reason = "The change affects code outside the package set containing the changed files."
            )
        }

        if (highRiskChanged.isNotEmpty()) {
            findings += PRFinding(
                ruleId = "CHANGED_HIGH_RISK_COMPONENT",
                severity = FindingSeverity.HIGH,
                paths = highRiskChanged.map { normalize(it.path) }.sorted(),
                evidence = mapOf("highRiskFiles" to highRiskChanged.size.toString()),
                reason = "The change directly modifies components with existing deterministic engineering-risk signals."
            )
        }

        val sourceChanges = changeSet.files.filter { it.changeType != ChangeType.DELETED }
        if (sourceChanges.isNotEmpty() && candidateTests.isEmpty()) {
            findings += PRFinding(
                ruleId = "TEST_CANDIDATE_MISSING",
                severity = FindingSeverity.MEDIUM,
                paths = sourceChanges.map { normalize(it.path) }.sorted(),
                evidence = mapOf("candidateTests" to "0"),
                reason = "No likely test candidates were identified for the changed source files. This is a review signal, not proof of missing coverage."
            )
        }

        val totalChangedLines = changeSet.addedLines + changeSet.deletedLines
        if (changeSet.files.size >= 20 || totalChangedLines >= 500) {
            findings += PRFinding(
                ruleId = "CHANGE_LARGE",
                severity = FindingSeverity.MEDIUM,
                paths = changeSet.files.map { normalize(it.path) }.sorted(),
                evidence = mapOf("filesChanged" to changeSet.files.size.toString(), "lineChanges" to totalChangedLines.toString()),
                reason = "The change is large enough that decomposition or focused review may reduce regression risk."
            )
        }

        findings.sortWith(compareByDescending<PRFinding> { severityRank(it.severity) }.thenBy { it.ruleId })
        return PRIntelligenceResult(
            changeSummary = summarize(changeSet),
            impactedFiles = impact.summary.impactedFiles,
            impactedPackages = impact.summary.impactedPackages,
            crossPackageImpacts = impact.summary.crossPackageImpacts,
            testCandidates = candidateTests,
            findings = findings,
            aggregateSeverity = findings.maxByOrNull { severityRank(it.severity) }?.severity ?: FindingSeverity.INFO
        )
    }

    private fun summarize(changeSet: ChangeSet): ChangeSummary = ChangeSummary(
        filesChanged = changeSet.files.size,
        filesAdded = changeSet.files.count { it.changeType == ChangeType.ADDED },
        filesModified = changeSet.files.count { it.changeType == ChangeType.MODIFIED },
        filesDeleted = changeSet.files.count { it.changeType == ChangeType.DELETED },
        filesRenamed = changeSet.files.count { it.changeType == ChangeType.RENAMED },
        filesCopied = changeSet.files.count { it.changeType == ChangeType.COPIED },
        additions = changeSet.addedLines,
        deletions = changeSet.deletedLines
    )

    private fun severityRank(severity: FindingSeverity): Int = severity.ordinal
    private fun normalize(path: String): String = path.replace('\\', '/')
}
