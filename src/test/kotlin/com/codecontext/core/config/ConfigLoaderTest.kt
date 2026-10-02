package com.codecontext.core.config

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigLoaderTest {
    private lateinit var tempDir: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        ConfigTestLock.lock.lock()
        tempDir = createTempDirectory("codecontext-config-loader-test")
        System.setProperty("codecontext.config.home", tempDir.toString())
    }

    @AfterTest
    fun tearDown() {
        try {
            System.clearProperty("codecontext.config.home")
            tempDir.toFile().deleteRecursively()
        } finally {
            ConfigTestLock.lock.unlock()
        }
    }

    @Test
    fun `effective config uses user credential without creating project config`() {
        UserConfigStore.saveAi("gemini", "user-key", "gemini-2.5-flash")
        val project = Files.createTempFile("codecontext-project", ".json")
        Files.writeString(project, "{\"ai\":{\"enabled\":false,\"provider\":\"gemini\",\"apiKey\":\"\",\"model\":\"gemini-2.5-flash\"}}")

        val config = ConfigLoader.loadEffective(project.toString())

        assertTrue(config.ai.enabled)
        assertEquals("gemini", config.ai.provider)
        assertEquals("user-key", config.ai.apiKey)
        assertTrue(!project.toFile().readText().contains("user-key"))
    }

    @Test
    fun `repository credential wins over user credential`() {
        UserConfigStore.saveAi("gemini", "user-key", "gemini-2.5-flash")
        val project = Files.createTempFile("codecontext-project", ".json")
        Files.writeString(project, "{\"ai\":{\"enabled\":true,\"provider\":\"gemini\",\"apiKey\":\"project-key\",\"model\":\"gemini-2.5-flash\"}}")

        val config = ConfigLoader.loadEffective(project.toString())
        assertEquals("project-key", config.ai.apiKey)
    }

    @Test
    fun `repository config is resolved from the target repository instead of process cwd`() {
        val repository = Files.createTempDirectory("codecontext-target-repo").toFile()
        try {
            repository.resolve(".codecontext.json").writeText(
                """{"maxFilesAnalyze":17,"hotspotCount":3}"""
            )
            val config = ConfigLoader.loadForRepository(repository.path)
            assertEquals(17, config.maxFilesAnalyze)
            assertEquals(3, config.hotspotCount)
        } finally {
            repository.deleteRecursively()
        }
    }
}
