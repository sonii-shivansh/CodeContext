package com.codecontext.server

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class PathSecurityTest {
    private val original = System.getProperty("user.dir")

    @AfterEach
    fun restore() { System.setProperty("user.dir", original) }

    @Test
    fun `rejects sibling path with allowed prefix`() {
        val root = createTempDirectory("codecontext-allowed").toFile()
        val sibling = File(root.parentFile, root.name + "-attacker").apply { mkdirs() }
        try {
            System.setProperty("user.dir", root.absolutePath)
            assertNull(sanitizePath(sibling.absolutePath))
        } finally {
            root.deleteRecursively()
            sibling.deleteRecursively()
        }
    }

    @Test
    fun `accepts a real directory inside the temporary workspace`() {
        val directory = createTempDirectory("codecontext-safe").toFile()
        try { assertNotNull(sanitizePath(directory.absolutePath)) } finally { directory.deleteRecursively() }
    }
}
