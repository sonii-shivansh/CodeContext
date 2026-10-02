package com.codecontext.core.workflow

import com.codecontext.core.intelligence.ChangeType
import com.codecontext.core.intelligence.ChangedFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChangeSafetyTest {
    @Test
    fun passesWhenAllChangedPathsArePlanned() {
        val result = ChangeSafetyAnalyzer.verify(listOf(ChangedFile("src/main/App.kt", ChangeType.MODIFIED)), listOf("src/main/App.kt"))
        assertEquals(SafetyStatus.PASS, result.status)
        assertTrue(result.unexpectedPaths.isEmpty())
    }

    @Test
    fun failsWhenAChangeFallsOutsideThePlan() {
        val result = ChangeSafetyAnalyzer.verify(listOf(ChangedFile("src/main/App.kt", ChangeType.MODIFIED), ChangedFile("src/main/Unplanned.kt", ChangeType.ADDED)), listOf("src/main/App.kt"))
        assertEquals(SafetyStatus.FAIL, result.status)
        assertEquals(listOf("src/main/Unplanned.kt"), result.unexpectedPaths)
    }

    @Test
    fun requestsReviewForDeletion() {
        val result = ChangeSafetyAnalyzer.verify(listOf(ChangedFile("src/main/Legacy.kt", ChangeType.DELETED)), listOf("src/main/Legacy.kt"))
        assertEquals(SafetyStatus.REVIEW_REQUIRED, result.status)
        assertEquals(listOf("src/main/Legacy.kt"), result.deletedPaths)
    }

    @Test
    fun ignoresGeneratedArtifactsFromSafetyScope() {
        val result = ChangeSafetyAnalyzer.verify(
            listOf(ChangedFile("output/engineering-context.json", ChangeType.MODIFIED), ChangedFile("src/main/App.kt", ChangeType.MODIFIED)),
            listOf("src/main/App.kt")
        )
        assertEquals(SafetyStatus.PASS, result.status)
        assertEquals(listOf("src/main/App.kt"), result.changedPaths)
    }

    @Test
    fun requestsReviewWhenSourceChangesHaveNoExplicitPlan() {
        val result = ChangeSafetyAnalyzer.verify(listOf(ChangedFile("src/main/App.kt", ChangeType.MODIFIED)), emptyList())
        assertEquals(SafetyStatus.REVIEW_REQUIRED, result.status)
        assertTrue(result.reasons.any { it.contains("No explicit source paths") })
    }
}
