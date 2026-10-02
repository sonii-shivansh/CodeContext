package com.codecontext.mcp

import com.codecontext.cli.CodeParallelParser
import com.codecontext.core.cache.CacheManager
import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.intelligence.ArchitectureContract
import com.codecontext.core.intelligence.ArchitectureContractEngine
import com.codecontext.core.intelligence.ArchitectureContractResult
import com.codecontext.core.intelligence.ArchitectureDriftEngine
import com.codecontext.core.intelligence.ArchitectureDriftResult
import com.codecontext.core.intelligence.ArchitectureIntelligenceEngine
import com.codecontext.core.intelligence.ArchitectureIntelligenceResult
import com.codecontext.core.intelligence.EngineeringContextEngine
import com.codecontext.core.intelligence.EngineeringContextSnapshot
import com.codecontext.core.intelligence.GitChangeSetBuilder
import com.codecontext.core.planner.EngineeringPlan
import com.codecontext.core.scanner.RepositoryScanner
import com.codecontext.core.workflow.AgentChangeContract
import com.codecontext.core.workflow.ChangeSafetyAnalyzer
import com.codecontext.core.workflow.EngineeringPreparation
import com.codecontext.core.workflow.EngineeringVerification
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

/** Shared MCP gateway over deterministic engineering-context workflows. */
object EngineeringContextGateway {
    private val json = Json { encodeDefaults = true; explicitNulls = false; prettyPrint = false }
    fun reality(repoPath: String): JsonObject { val root = repository(repoPath); val config = ConfigLoader.loadForRepository(root.path); return json.parseToJsonElement(EngineeringContextEngine.encode(EngineeringContextEngine.snapshot(root, RepositoryScanner(config)))).jsonObject }
    fun snapshot(repoPath: String): JsonObject = reality(repoPath)
    fun diff(before: JsonObject, after: JsonObject): JsonObject { val b = json.decodeFromJsonElement(EngineeringContextSnapshot.serializer(), before); val a = json.decodeFromJsonElement(EngineeringContextSnapshot.serializer(), after); return json.parseToJsonElement(EngineeringContextEngine.encode(EngineeringContextEngine.diff(b, a))).jsonObject }
    fun architectureDrift(repoPath: String, baseline: JsonObject): JsonObject { val root = repository(repoPath); val baselineResult = json.decodeFromJsonElement(ArchitectureIntelligenceResult.serializer(), baseline); val config = ConfigLoader.loadForRepository(root.path); val parsed = runBlocking { CodeParallelParser(CacheManager()).parseFiles(RepositoryScanner(config).scan(root.path)) }; val graph = RobustDependencyGraph(); graph.build(parsed).getOrThrow(); graph.analyze().getOrThrow(); val drift: ArchitectureDriftResult = ArchitectureDriftEngine.compare(baselineResult, ArchitectureIntelligenceEngine.analyze(graph.graph, root, config.architecture)); return json.encodeToJsonElement(ArchitectureDriftResult.serializer(), drift).jsonObject }
    fun architectureContract(repoPath: String, contract: JsonObject?): JsonObject { val root = repository(repoPath); val config = ConfigLoader.loadForRepository(root.path); val parsed = runBlocking { CodeParallelParser(CacheManager()).parseFiles(RepositoryScanner(config).scan(root.path)) }; val graph = RobustDependencyGraph(); graph.build(parsed).getOrThrow(); graph.analyze().getOrThrow(); val architecture = ArchitectureIntelligenceEngine.analyze(graph.graph, root, config.architecture); val contractValue = contract?.let { json.decodeFromJsonElement(ArchitectureContract.serializer(), it) } ?: root.resolve(".codecontext-architecture-contract.json").takeIf { it.exists() }?.let { json.decodeFromString<ArchitectureContract>(it.readText()) } ?: ArchitectureContract(); return json.encodeToJsonElement(ArchitectureContractResult.serializer(), ArchitectureContractEngine.evaluate(architecture, contractValue)).jsonObject }
    fun prepare(repoPath: String, changeSummary: String): JsonObject { val result = runBlocking { EngineeringPreparation.prepare(repository(repoPath).path, changeSummary) }; return json.encodeToJsonElement(com.codecontext.core.workflow.EngineeringPreparationResult.serializer(), result).jsonObject }
    fun changeContract(repoPath: String, changeSummary: String): JsonObject { val result = runBlocking { EngineeringPreparation.prepare(repository(repoPath).path, changeSummary) }; return json.encodeToJsonElement(AgentChangeContract.serializer(), result.contract).jsonObject }
    fun evidence(repoPath: String, changeSummary: String): JsonObject { val result = runBlocking { EngineeringPreparation.prepare(repository(repoPath).path, changeSummary) }; return json.encodeToJsonElement(com.codecontext.core.ai.GroundedEvidence.serializer(), result.evidence).jsonObject }
    fun changeSafety(repoPath: String, plan: JsonObject): JsonObject { val root = repository(repoPath); val engineeringPlan = json.decodeFromJsonElement(EngineeringPlan.serializer(), plan); val changeSet = GitChangeSetBuilder.fromWorkingTree(root.path); val result = ChangeSafetyAnalyzer.verify(changeSet.files, engineeringPlan.plannedPaths.ifEmpty { engineeringPlan.affectedComponents }); return json.encodeToJsonElement(com.codecontext.core.workflow.ChangeSafetyResult.serializer(), result).jsonObject }
    fun verify(repoPath: String, plan: JsonObject, contract: JsonObject? = null): JsonObject {
        val root = repository(repoPath)
        val engineeringPlan = json.decodeFromJsonElement(EngineeringPlan.serializer(), plan)
        val persisted = contract ?: root.resolve("output/agent-change-contract.json").takeIf { it.isFile }?.let { json.decodeFromString<AgentChangeContract>(it.readText()).let { value -> json.encodeToJsonElement(AgentChangeContract.serializer(), value).jsonObject } }
        require(persisted != null) { "Immutable agent change contract is required; run prepare first." }
        val preparedContract = json.decodeFromJsonElement(AgentChangeContract.serializer(), persisted)
        val result = runBlocking { EngineeringVerification.verify(root.path, engineeringPlan, preparedContract) }
        return json.encodeToJsonElement(com.codecontext.core.workflow.EngineeringVerificationResult.serializer(), result).jsonObject
    }
    private fun repository(path: String): File { require(!path.startsWith("http://", true) && !path.startsWith("https://", true)) { "Remote repositories are not supported" }; val root = File(path).canonicalFile; require(root.isDirectory) { "Repository path is not a directory: $path" }; return root }
}
