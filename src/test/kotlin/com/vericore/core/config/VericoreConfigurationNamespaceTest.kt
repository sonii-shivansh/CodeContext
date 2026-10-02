package com.vericore.core.config

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VericoreConfigurationNamespaceTest {
    private lateinit var tempDir: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        ConfigTestLock.lock.lock()
        tempDir = createTempDirectory("vericore-config-test")
        System.setProperty("vericore.config.home", tempDir.toString())
        System.clearProperty("codecontext.config.home")
    }

    @AfterTest
    fun tearDown() {
        try {
            System.clearProperty("vericore.config.home")
            System.clearProperty("codecontext.config.home")
            tempDir.toFile().deleteRecursively()
        } finally {
            ConfigTestLock.lock.unlock()
        }
    }

    @Test
    fun `canonical repository config is preferred and legacy config is fallback`() {
        val repository = Files.createTempDirectory("vericore-target-repo").toFile()
        try {
            repository.resolve(".codecontext.json").writeText("{\"maxFilesAnalyze\":17}")
            repository.resolve(".vericore.json").writeText("{\"maxFilesAnalyze\":23}")

            assertEquals(23, ConfigLoader.loadForRepository(repository.path).maxFilesAnalyze)

            repository.resolve(".vericore.json").delete()
            assertEquals(17, ConfigLoader.loadForRepository(repository.path).maxFilesAnalyze)
        } finally {
            repository.deleteRecursively()
        }
    }

    @Test
    fun `createDefault writes canonical vericore config`() {
        val path = tempDir.resolve(".vericore.json")

        ConfigLoader.createDefault(path.toString())

        assertTrue(Files.isRegularFile(path))
        assertTrue(Files.readString(path).contains("\"maxFilesAnalyze\""))
    }

    @Test
    fun `canonical environment namespace takes precedence over legacy namespace`() {
        val project = Files.createTempFile(tempDir, "vericore-project", ".json")
        Files.writeString(project, "{\"ai\":{\"enabled\":false,\"provider\":\"gemini\",\"apiKey\":\"\",\"model\":\"project-model\"}}")

        val config = ConfigLoader.loadEffective(
            project.toString(),
            environment = mapOf(
                "VERICORE_AI_PROVIDER" to "vericore-provider",
                "CODECONTEXT_AI_PROVIDER" to "legacy-provider",
                "VERICORE_AI_MODEL" to "vericore-model",
                "CODECONTEXT_AI_MODEL" to "legacy-model"
            )
        )

        assertEquals("vericore-provider", config.ai.provider)
        assertEquals("vericore-model", config.ai.model)
    }

    @Test
    fun `user config is saved to canonical home and legacy home remains readable`() {
        UserConfigStore.saveAi("gemini", "canonical-key", "gemini-3.8-flash")
        assertTrue(UserConfigStore.configFile().toPath().startsWith(tempDir))
        assertEquals("canonical-key", requireNotNull(UserConfigStore.load()).ai.apiKey)

        val legacyHome = tempDir.resolve("legacy-codecontext")
        Files.createDirectories(legacyHome)
        Files.writeString(
            legacyHome.resolve("config.json"),
            """{"ai":{"provider":"gemini","apiKey":"legacy-key","model":"gemini-2.5-flash"}}"""
        )
        Files.deleteIfExists(tempDir.resolve("config.json"))
        System.clearProperty("vericore.config.home")
        System.setProperty("codecontext.config.home", legacyHome.toString())

        assertEquals("legacy-key", requireNotNull(UserConfigStore.load()).ai.apiKey)
    }
}
