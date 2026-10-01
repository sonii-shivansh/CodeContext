package com.codecontext.core.workflow

import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.intelligence.AnalysisSnapshotBuilder
import com.codecontext.core.intelligence.ArchitectureIntelligenceEngine
import com.codecontext.core.intelligence.ArchitectureIntelligenceResult
import com.codecontext.core.intelligence.ChangeImpactEngine
import com.codecontext.core.intelligence.DecisionProvenance
import com.codecontext.core.intelligence.GitChangeSetBuilder
import com.codecontext.core.scanner.OptimizedGitAnalyzer
import com.codecontext.core.scanner.RepositoryScanner
import com.codecontext.cli.CodeParallelParser
import com.codecontext.core.cache.CacheManager
import com.codecontext.core.planner.EngineeringPlan
import java.io.File
import kotlinx.serialization.Serializable

@Serializable
data class EngineeringVerificationResult(
    val schemaVersion: String = "1.0",
    val repository: String,
    val safety: ChangeSafetyResult,
    val prIntelligence: com.codecontext.core.intelligence.PRIntelligenceResult,
    val architecture: ArchitectureIntelligenceResult,
    val verificationCommands: List<String>,
    val status: SafetyStatus,
    val provenance: DecisionProvenance = DecisionProvenance.create("verify", null, "1.0", emptyList())
)

/** Runs deterministic post-change checks against the current working tree. */
object EngineeringVerification {
    suspend fun verify(repoPath: String, plan: EngineeringPlan): EngineeringVerificationResult {
        val root = File(repoPath).canonicalFile
        require(root.isDirectory) { "Repository path is not a directory: $repoPath" }
        val config = ConfigLoader.load()
        val files = RepositoryScanner(config).scan(root.path)
        require(files.size <= config.maxFilesAnalyze) { "Repository exceeds the maximum file limit: ${config.maxFilesAnalyze}" }
        val parsed = CodeParallelParser(CacheManager()).parseFiles(files)
        val enriched = OptimizedGitAnalyzer().analyze(root.path, parsed)
        val graph = RobustDependencyGraph()
        graph.build(enriched).getOrThrow()
        graph.analyze().getOrThrow()

        val changeSet = GitChangeSetBuilder.fromWorkingTree(root.path)
        val safety = ChangeSafetyAnalyzer.verify(changeSet.files, plan.affectedComponents)
        val packageByPath = enriched.associate { file ->
            root.toPath().relativize(file.file.toPath().toAbsolutePath().normalize()).toString().replace('\\', '/') to file.packageName
        }
        val absoluteByRelative = enriched.associate { file ->
            root.toPath().relativize(file.file.toPath().toAbsolutePath().normalize()).toString().replace('\\', '/') to file.file.absolutePath.replace('\\', '/')
        }
        val changedAbsolute = changeSet.files.mapNotNull { absoluteByRelative[it.path] }
        val churn = enriched.associate { it.file.absolutePath.replace('\\', '/') to it.gitMetadata.changeFrequency }
        val packages = enriched.associate { it.file.absolutePath.replace('\\', '/') to it.packageName }
        val impact = ChangeImpactEngine.analyze(graph.graph, changedAbsolute, graph.pageRankScores, churn, packages)
        val snapshot = AnalysisSnapshotBuilder.build(root.path, enriched, graph.graph, graph.pageRankScores, graph.hasCycles)
        val risks = com.codecontext.core.intelligence.EngineeringRiskEngine.calculate(snapshot)
        val tests = impact.nodes.filter { it.relationship == com.codecontext.core.intelligence.ImpactRelationship.TEST_CANDIDATE }.map { root.toPath().relativize(File(it.path).toPath()).toString().replace('\\', '/') }
        val pr = com.codecontext.core.intelligence.PRIntelligenceEngine.analyze(
            changeSet = changeSet,
            impact = impact,
            risks = risks,
            packageByPath = packageByPath,
            testCandidates = tests,
            pathMapper = { path -> root.toPath().relativize(File(path).toPath()).toString().replace('\\', '/') }
        )
        val architecture = ArchitectureIntelligenceEngine.analyze(graph.graph, root, config.architecture)
        val status = when {
            safety.status == SafetyStatus.FAIL -> SafetyStatus.FAIL
            safety.status == SafetyStatus.REVIEW_REQUIRED || pr.aggregateSeverity.name == "CRITICAL" -> SafetyStatus.REVIEW_REQUIRED
            else -> SafetyStatus.PASS
        }
        val provenance = DecisionProvenance.capture(
            repoPath = root.path,
            operation = "verify",
            analysisSchemaVersion = snapshot.schemaVersion,
            evidenceIds = plan.evidenceIds
        )
        return EngineeringVerificationResult(
            repository = root.path,
            safety = safety,
            prIntelligence = pr,
            architecture = architecture,
            verificationCommands = plan.verificationCommands,
            status = status,
            provenance = provenance
        )
    }
}
