package com.vericore.core.workflow

import com.vericore.core.planner.EngineeringPlan
import com.vericore.core.planner.RiskLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AgentChangeContractTest {
    private fun plan(path: String, repository: String = "") = EngineeringPlan(
        changeSummary = "Update service behavior",
        repository = repository,
        affectedComponents = listOf(path),
        plannedPaths = listOf(path),
        concerns = listOf("Review architecture evidence before implementation."),
        riskLevel = RiskLevel.MEDIUM,
        steps = emptyList(),
        verificationCommands = listOf("./gradlew test"),
        evidenceIds = listOf("e-1"),
        uncertainties = emptyList()
    )

    @Test
    fun `same plan produces stable fingerprint`() {
        assertEquals(AgentChangeContract.fingerprintFor(plan("src/App.kt")), AgentChangeContract.fingerprintFor(plan("src/App.kt")))
    }

    @Test
    fun `different planned path changes fingerprint`() {
        assertNotEquals(AgentChangeContract.fingerprintFor(plan("src/App.kt")), AgentChangeContract.fingerprintFor(plan("src/Other.kt")))
    }

    @Test
    fun `contract captures plan scope and verification`() {
        val contract = AgentChangeContract.fromPlan(plan("src/App.kt", "/tmp/example-repo"))
        assertEquals(listOf("src/App.kt"), contract.plannedPaths)
        assertEquals(listOf("./gradlew test"), contract.verificationCommands)
        assertEquals(java.io.File("/tmp/example-repo").canonicalPath, contract.repository)
        assertEquals(64, contract.fingerprint.length)
    }

    @Test
    fun `repository scope changes fingerprint`() {
        val first = AgentChangeContract.fromPlan(plan("src/App.kt", "/tmp/repo-one"))
        val second = AgentChangeContract.fromPlan(plan("src/App.kt", "/tmp/repo-two"))
        assertNotEquals(first.fingerprint, second.fingerprint)
    }

    @Test
    fun `fingerprint covers expected change types`() {
        val base = AgentChangeContract.fromPlan(plan("src/App.kt"))
        val changed = base.copy(expectedChangeTypes = mapOf("src/App.kt" to listOf("MODIFIED")))
        assertNotEquals(AgentChangeContract.fingerprintFor(base), AgentChangeContract.fingerprintFor(changed))
    }

    @Test
    fun `fingerprint covers schema version`() {
        val base = AgentChangeContract.fromPlan(plan("src/App.kt"))
        val changed = base.copy(schemaVersion = "3.0")
        assertNotEquals(AgentChangeContract.fingerprintFor(base), AgentChangeContract.fingerprintFor(changed))
    }

    @Test
    fun `fingerprint is independent of change type map insertion order`() {
        val base = AgentChangeContract.fromPlan(plan("src/App.kt"))
        val first = base.copy(expectedChangeTypes = linkedMapOf("src/App.kt" to listOf("MODIFIED"), "src/Other.kt" to listOf("ADDED")))
        val second = base.copy(expectedChangeTypes = linkedMapOf("src/Other.kt" to listOf("ADDED"), "src/App.kt" to listOf("MODIFIED")))
        assertEquals(AgentChangeContract.fingerprintFor(first), AgentChangeContract.fingerprintFor(second))
    }
}
