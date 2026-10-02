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
    val schemaVersion: String = "1.1",
    val repository: String,
    val safety: ChangeSafetyResult,
    val prIntelligence: com.codecontext.core.intelligence.PRIntelligenceResult,
    val architecture: ArchitectureIntelligenceResult,
    val verificationCommands: List<String>,
    val status: SafetyStatus,
    val provenance: DecisionProvenance = DecisionProvenance.create("verify", null, "1.0", emptyList()),
    val contract: AgentChangeContractResult? = null
)

/** Runs deterministic post-change checks against the exact persisted contract produced by prepare. */
object EngineeringVerification {
    suspend fun verify(repoPath: String, plan: EngineeringPlan): EngineeringVerificationResult =
        verify(repoPath, plan, AgentChangeContract.fromPlan(plan))

    suspend fun verify(repoPath: String, plan: EngineeringPlan, contract: AgentChangeContract): EngineeringVerificationResult {
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

        val changeSet = GitChangeSetBuilder.fromWorkingTree(root.path)
        val plannedPaths = if (plan.plannedPaths.isNotEmpty()) plan.plannedPaths else plan.affectedComponents
        val safety = ChangeSafetyAnalyzer.verify(changeSet.files, plannedPaths)
        val packageByPath = enriched.associate { file -> root.toPath().relativize(file.file.toPath().toAbsolutePath().normalize()).toString().replace('\\', '/') to file.packageName }
        val absoluteByRelative = enriched.associate { file -> root.toPath().relativize(file.file.toPath().toAbsolutePath().normalize()).toString().replace('\\', '/') to file.file.absolutePath.replace('\\', '/') }
        val changedAbsolute = changeSet.files.mapNotNull { absoluteByRelative[it.path] }
        val churn = enriched.associate { it.file.absolutePath.replace('\\', '/') to it.gitMetadata.changeFrequency }
        val packages = enriched.associate { it.file.absolutePath.replace('\\', '/') to it.packageName }
        val impact = ChangeImpactEngine.analyze(graph.graph, changedAbsolute, graph.pageRankScores, churn, packages)
        val snapshot = AnalysisSnapshotBuilder.build(root.path, enriched, graph.graph, graph.pageRankScores, graph.hasCycles)
        val risks = com.codecontext.core.intelligence.EngineeringRiskEngine.calculate(snapshot)
        val toRelativePath: (String) -> String = { path ->
            val candidate = File(path).toPath()
            val absolute = if (candidate.isAbsolute) candidate else root.toPath().resolve(candidate)
            root.toPath().relativize(absolute.normalize()).toString().replace('\\', '/')
        }
        val tests = impact.nodes.filter { it.relationship == com.codecontext.core.intelligence.ImpactRelationship.TEST_CANDIDATE }.map { toRelativePath(it.path) }
        val pr = com.codecontext.core.intelligence.PRIntelligenceEngine.analyze(changeSet, impact, risks, packageByPath, tests, toRelativePath)
        val architecture = ArchitectureIntelligenceEngine.analyze(graph.graph, root, config.architecture)

        val currentHead = RepositoryState.head(root.path).orEmpty()
        val expectedContractFingerprint = AgentChangeContract.fingerprintFor(contract)
        val expectedPlanFingerprint = AgentChangeContract.fromPlan(plan, root.path, contract.preparedHead).fingerprint
        val reasons = buildList {
            if (contract.fingerprint != expectedContractFingerprint) add("The persisted agent change contract fingerprint is invalid or tampered.")
            if (contract.repository != root.path) add("The contract belongs to a different repository: ${contract.repository}")
            if (plan.contractFingerprint != contract.fingerprint) add("The engineering plan is not bound to the persisted contract fingerprint.")
            if (expectedPlanFingerprint != contract.fingerprint) add("The supplied plan does not match the persisted contract contents.")
            if (contract.preparedHead.isNotBlank() && currentHead.isNotBlank() && contract.preparedHead != currentHead) add("The repository HEAD changed after prepare (${contract.preparedHead} -> $currentHead); the contract is stale.")
        }
        val contractValid = reasons.isEmpty()
        val contractResult = AgentChangeContractResult(contract, contractValid, reasons)
        val status = when {
            !contractValid -> SafetyStatus.FAIL
            safety.status == SafetyStatus.FAIL -> SafetyStatus.FAIL
            safety.status == SafetyStatus.REVIEW_REQUIRED || pr.aggregateSeverity.name == "CRITICAL" -> SafetyStatus.REVIEW_REQUIRED
            else -> SafetyStatus.PASS
        }
        val provenance = DecisionProvenance.capture(root.path, "verify", snapshot.schemaVersion, contract.evidenceIds)
        return EngineeringVerificationResult(
            repository = root.path, safety = safety, prIntelligence = pr, architecture = architecture,
            verificationCommands = contract.verificationCommands, status = status, provenance = provenance, contract = contractResult
        )
    }
}
