package com.codecontext.core.workflow

import com.codecontext.core.intelligence.ChangedFile
import kotlinx.serialization.Serializable

@Serializable
data class ChangeSafetyResult(
    val schemaVersion: String = "1.0",
    val changedPaths: List<String>,
    val plannedPaths: List<String>,
    val unexpectedPaths: List<String>,
    val deletedPaths: List<String>,
    val status: SafetyStatus,
    val reasons: List<String>
)

@Serializable
enum class SafetyStatus { PASS, REVIEW_REQUIRED, FAIL }

/** Compares the actual working-tree scope with an evidence-backed plan. */
object ChangeSafetyAnalyzer {
    fun verify(changes: List<ChangedFile>, plannedPaths: Collection<String>): ChangeSafetyResult {
        val actual = changes.map { normalize(it.path) }.distinct().sorted()
        val planned = plannedPaths.map(::normalize).filter { it.isNotBlank() }.distinct().sorted()
        val plannedSet = planned.toSet()
        val unexpected = actual.filterNot(plannedSet::contains)
        val deleted = changes.filter { it.changeType.name == "DELETED" }.map { normalize(it.path) }.distinct().sorted()

        val reasons = buildList {
            if (actual.isEmpty()) add("No working-tree changes were detected.")
            if (unexpected.isNotEmpty()) add("One or more changed paths are outside the supplied engineering plan.")
            if (deleted.isNotEmpty()) add("Deleted files require explicit review before the change is considered safe.")
        }
        val status = when {
            unexpected.isNotEmpty() -> SafetyStatus.FAIL
            deleted.isNotEmpty() -> SafetyStatus.REVIEW_REQUIRED
            else -> SafetyStatus.PASS
        }
        return ChangeSafetyResult(
            changedPaths = actual,
            plannedPaths = planned,
            unexpectedPaths = unexpected,
            deletedPaths = deleted,
            status = status,
            reasons = reasons
        )
    }

    private fun normalize(path: String): String = path.replace('\\', '/').trim().removePrefix("./")
}
