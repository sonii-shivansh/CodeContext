package com.vericore.core.workflow

import com.vericore.core.planner.EngineeringPlan
import com.vericore.core.planner.RiskLevel
import kotlinx.serialization.json.Json
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import java.io.File

class ImmutableAgentChangeContractTest {
    private fun plan(path: String) = EngineeringPlan(
        changeSummary = "Update service behavior",
        affectedComponents = listOf(path),
        plannedPaths = listOf(path),
        concerns = listOf("Preserve architecture boundaries."),
        riskLevel = RiskLevel.MEDIUM,
        steps = emptyList(),
        verificationCommands = listOf("./gradlew test"),
        evidenceIds = listOf("e-1"),
        uncertainties = emptyList()
    )

    @Test
    fun `contract binds repository and prepared head`() {
        val root = createTempDirectory("contract").toFile()
        val contract = AgentChangeContract.fromPlan(plan("src/App.kt"), root.path, "0123456789012345678901234567890123456789")
        assertEquals(root.canonicalPath, contract.repository)
        assertEquals("0123456789012345678901234567890123456789", contract.preparedHead)
        assertEquals(64, contract.fingerprint.length)
    }

    @Test
    fun `tampering contract fields invalidates fingerprint`() {
        val contract = AgentChangeContract.fromPlan(plan("src/App.kt"), "/repo", "abc")
        val tampered = contract.copy(plannedPaths = listOf("src/Other.kt"))
        assertNotEquals(contract.fingerprint, AgentChangeContract.fingerprintFor(tampered))
    }

    @Test
    fun `serialized contract round trips without changing fingerprint`() {
        val contract = AgentChangeContract.fromPlan(plan("src/App.kt"), "/repo", "abc")
        val json = Json { encodeDefaults = true }
        val restored = json.decodeFromString<AgentChangeContract>(json.encodeToString(contract))
        assertEquals(contract, restored)
        assertEquals(contract.fingerprint, AgentChangeContract.fingerprintFor(restored))
    }

    @Test
    fun `repository head is captured for git repositories`() {
        val root = createTempDirectory("repo-state").toFile()
        ProcessBuilder("git", "init").directory(root).inheritIO().start().waitFor()
        File(root, "README.md").writeText("test")
        ProcessBuilder("git", "add", ".").directory(root).inheritIO().start().waitFor()
        ProcessBuilder("git", "-c", "user.name=CodeContext", "-c", "user.email=ci@example.com", "commit", "-m", "test").directory(root).inheritIO().start().waitFor()
        val head = RepositoryState.head(root.path)
        assertTrue(head?.matches(Regex("[0-9a-f]{40}")) == true)
        root.deleteRecursively()
    }
}
