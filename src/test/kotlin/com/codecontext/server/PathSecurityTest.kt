package com.codecontext.server

import java.io.File
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PathSecurityTest {
    @Test
    fun `rejects sibling path with allowed prefix`() {
        val allowedRoot = File(System.getProperty("user.dir"), ".codecontext-path-security-${System.nanoTime()}")
        check(allowedRoot.mkdirs()) { "Could not create test directory" }
        val sibling = File(allowedRoot.parentFile, allowedRoot.name + "-attacker").apply { mkdirs() }
        try {
            // The sibling shares the textual prefix of the repository path but is not inside it.
            assertNull(sanitizePath(sibling.absolutePath))
        } finally {
            allowedRoot.deleteRecursively()
            sibling.deleteRecursively()
        }
    }

    @Test
    fun `accepts a real directory inside the temporary workspace`() {
        val directory = kotlin.io.path.createTempDirectory("codecontext-safe").toFile()
        try {
            assertNotNull(sanitizePath(directory.absolutePath))
        } finally {
            directory.deleteRecursively()
        }
    }
}
