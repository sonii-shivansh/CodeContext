package com.codecontext.core.config

import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserConfigStoreTest {
    private lateinit var tempDir: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory("codecontext-config-test")
        System.setProperty("codecontext.config.home", tempDir.toString())
    }

    @AfterTest
    fun tearDown() {
        System.clearProperty("codecontext.config.home")
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `credentials are stored outside project and can be loaded`() {
        UserConfigStore.saveAi("gemini", "test-key", "gemini-2.5-flash")

        val loaded = requireNotNull(UserConfigStore.load())
        assertEquals("gemini", loaded.ai.provider)
        assertEquals("test-key", loaded.ai.apiKey)
        assertEquals("gemini-2.5-flash", loaded.ai.model)
        assertTrue(UserConfigStore.configFile().toPath().startsWith(tempDir))
    }

    @Test
    fun `delete removes saved credentials`() {
        UserConfigStore.saveAi("gemini", "test-key", "gemini-2.5-flash")
        assertTrue(UserConfigStore.delete())
        assertEquals(null, UserConfigStore.load())
    }
}
