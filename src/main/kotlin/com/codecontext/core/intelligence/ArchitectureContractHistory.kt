package com.codecontext.core.intelligence

import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.eclipse.jgit.api.Git

const val ARCHITECTURE_CONTRACT_HISTORY_SCHEMA_VERSION = "1.0"

@Serializable
data class ArchitectureContractDecision(
    val schemaVersion: String,
    val decisionId: String,
    val repositoryCommit: String?,
    val contractDigest: String,
    val resultDigest: String,
    val passed: Boolean,
    val violationCount: Int
)

@Serializable
data class ArchitectureContractHistory(
    val schemaVersion: String,
    val decisions: List<ArchitectureContractDecision>
)

object ArchitectureContractHistoryStore {
    private val json = Json { encodeDefaults = true; prettyPrint = true; ignoreUnknownKeys = false }

    fun decision(root: File, contract: ArchitectureContract, result: ArchitectureContractResult): ArchitectureContractDecision {
        val contractJson = json.encodeToString(ArchitectureContract.serializer(), contract)
        val resultJson = json.encodeToString(ArchitectureContractResult.serializer(), result)
        val contractDigest = sha256(contractJson)
        val resultDigest = sha256(resultJson)
        val commit = runCatching { Git.open(root).use { it.repository.resolve("HEAD")?.name } }.getOrNull()
        val identity = listOf(commit.orEmpty(), contractDigest, resultDigest, result.passed, result.violations.size).joinToString("|")
        return ArchitectureContractDecision(
            schemaVersion = ARCHITECTURE_CONTRACT_HISTORY_SCHEMA_VERSION,
            decisionId = sha256(identity),
            repositoryCommit = commit,
            contractDigest = contractDigest,
            resultDigest = resultDigest,
            passed = result.passed,
            violationCount = result.violations.size
        )
    }

    fun record(file: File, decision: ArchitectureContractDecision) {
        val existing = if (file.exists()) {
            json.decodeFromString(ArchitectureContractHistory.serializer(), file.readText())
        } else {
            ArchitectureContractHistory(ARCHITECTURE_CONTRACT_HISTORY_SCHEMA_VERSION, emptyList())
        }
        require(existing.schemaVersion == ARCHITECTURE_CONTRACT_HISTORY_SCHEMA_VERSION) {
            "Unsupported architecture contract history schema: ${existing.schemaVersion}"
        }
        val decisions = (existing.decisions + decision).distinctBy { it.decisionId }.sortedBy { it.decisionId }
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(ArchitectureContractHistory.serializer(), ArchitectureContractHistory(ARCHITECTURE_CONTRACT_HISTORY_SCHEMA_VERSION, decisions)))
    }

    fun encode(decision: ArchitectureContractDecision): String =
        json.encodeToString(ArchitectureContractDecision.serializer(), decision)

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
