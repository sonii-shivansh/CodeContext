package com.codecontext.core.workflow

import com.codecontext.cli.CodeParallelParser
import com.codecontext.core.ai.GroundedEvidence
import com.codecontext.core.ai.GroundedEvidenceBuilder
import com.codecontext.core.cache.CacheManager
import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.intelligence.AnalysisSnapshotBuilder
import com.codecontext.core.intelligence.ChangeSet
import com.codecontext.core.intelligence.DecisionProvenance
import com.codecontext.core.planner.EngineeringPlan
import com.codecontext.core.planner.EngineeringPlanRequest
import com.codecontext.core.planner.EngineeringPlanner
import com.codecontext.core.scanner.OptimizedGitAnalyzer
import com.codecontext.core.scanner.RepositoryScanner
import java.io.File
import kotlinx.serialization.Serializable

@Serializable
data class EngineeringPreparationResult(
    val schemaVersion: String = "1.1",
    val repository: String,
    val changeSet: ChangeSet,
    val evidence: GroundedEvidence,
    val plan: EngineeringPlan,
    val contract: AgentChangeContract,
    val provenance: DecisionProvenance = DecisionProvenance.create("prepare", null, "1.0", emptyList())
)

/** Builds a reusable evidence snapshot, deterministic plan, and immutable change contract before coding. */
object EngineeringPreparation {
    suspend fun prepare(repoPath: String, changeSummary: String): EngineeringPreparationResult {
        val root = File(repoPath).canonicalFile
        require(root.isDirectory) { "Repository path is not a directory: $repoPath" }
        val config = ConfigLoader.loadForRepository(root.path)
        val files = RepositoryScanner(config).scan(root.path)
        require(files.size <= config.maxFilesAnalyze) { "Repository exceeds the maximum file limit: ${config.maxFilesAnalyze}" }
        val parsed = CodeParallelParser(CacheManager()).parseFiles(files)
        val enriched = OptimizedGitAnalyzer().analyze(root.path, parsed)
        val graph = RobustDependencyGraph()
        graph.build(enriched).getOrThrow()
        graph.analyze().getOrThrow()
        val snapshot = AnalysisSnapshotBuilder.build(
            repositoryPath = root.path,
            parsedFiles = enriched,
            graph = graph.graph,
            pageRankScores = graph.pageRankScores,
            hasCycles = graph.hasCycles,
            parseFailures = 0
        )
        val evidence = GroundedEvidenceBuilder.fromSnapshot(snapshot)
        val changeSet = runCatching { com.codecontext.core.intelligence.GitChangeSetBuilder.fromWorkingTree(root.path) }
            .getOrElse { ChangeSet(emptyList(), source = "not-a-git-change-set") }
        val initialPlan = EngineeringPlanner().plan(
            EngineeringPlanRequest(
                changeSummary = changeSummary,
                changedPaths = changeSet.files.map { it.path },
                evidence = evidence
            )
        )
        val contract = AgentChangeContract.fromPlan(initialPlan)
        val plan = initialPlan.copy(contractFingerprint = contract.fingerprint)
        val provenance = DecisionProvenance.capture(
            repoPath = root.path,
            operation = "prepare",
            analysisSchemaVersion = snapshot.schemaVersion,
            evidenceIds = evidence.citations.map { it.id }
        )
        return EngineeringPreparationResult(
            repository = root.path,
            changeSet = changeSet,
            evidence = evidence,
            plan = plan,
            contract = contract,
            provenance = provenance
        )
    }
}
