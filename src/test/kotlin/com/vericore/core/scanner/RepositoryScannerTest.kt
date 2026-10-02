package com.vericore.core.scanner

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepositoryScannerTest {
    @Test
    fun `default scanner loads config from target repository and excludes generated output`() {
        val root = Files.createTempDirectory("codecontext-scanner-").toFile()
        try {
            root.resolve("src/main/App.kt").apply {
                parentFile.mkdirs()
                writeText("class App")
            }
            root.resolve("output/generated.kt").apply {
                parentFile.mkdirs()
                writeText("class Generated")
            }
            root.resolve(".codecontext/cache.kt").apply {
                parentFile.mkdirs()
                writeText("class Cache")
            }

            val scanner = RepositoryScanner()
            val files = scanner.scan(root.path)

            assertEquals(listOf("App.kt"), files.map { it.name })
            assertTrue(files.single().canonicalPath.startsWith(root.canonicalPath))
        } finally {
            root.deleteRecursively()
        }
    }
}
