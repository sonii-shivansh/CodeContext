package com.vericore.core.intelligence

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.io.File
import kotlinx.serialization.Serializable
import org.eclipse.jgit.api.Git

@Serializable
data class DecisionProvenance(
    val schemaVersion: String = "1.0",
    val id: String,
    val operation: String,
    val repositoryCommit: String? = null,
    val analysisSchemaVersion: String,
    val evidenceIds: List<String>,
    val unknownReason: String? = null
) {
    init {
        require(operation.isNotBlank()) { "operation must not be blank" }
        require(analysisSchemaVersion.isNotBlank()) { "analysisSchemaVersion must not be blank" }
    }

    companion object {
        fun create(
            operation: String,
            repositoryCommit: String?,
            analysisSchemaVersion: String,
            evidenceIds: List<String>
        ): DecisionProvenance {
            val orderedEvidenceIds = evidenceIds.distinct().sorted()
            val unknownReason = if (repositoryCommit == null) "repository commit unavailable" else null
            val identity = listOf(
                operation,
                repositoryCommit.orEmpty(),
                analysisSchemaVersion,
                orderedEvidenceIds.joinToString(",")
            ).joinToString("\n")
            return DecisionProvenance(
                id = sha256(identity),
                operation = operation,
                repositoryCommit = repositoryCommit,
                analysisSchemaVersion = analysisSchemaVersion,
                evidenceIds = orderedEvidenceIds,
                unknownReason = unknownReason
            )
        }

        fun capture(
            repoPath: String,
            operation: String,
            analysisSchemaVersion: String,
            evidenceIds: List<String>
        ): DecisionProvenance {
            val commit = runCatching {
                Git.open(File(repoPath)).use { git -> git.repository.resolve("HEAD")?.name }
            }.getOrNull()
            return create(operation, commit, analysisSchemaVersion, evidenceIds)
        }

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
