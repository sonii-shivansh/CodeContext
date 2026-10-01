package com.codecontext.core.scanner

import com.codecontext.core.config.CodeContextConfig
import com.codecontext.core.config.ConfigLoader
import java.io.File

class RepositoryScanner(
    private val configuredConfig: CodeContextConfig? = null
) {
    fun scan(rootPath: String): List<File> {
        val root = File(rootPath).canonicalFile
        if (!root.exists() || !root.isDirectory) {
            throw IllegalArgumentException("Invalid repository path: $rootPath")
        }

        // When callers do not explicitly supply configuration, resolve it from the
        // repository being analyzed rather than from CodeContext's process cwd.
        val config = configuredConfig ?: ConfigLoader.loadForRepository(root.path)
        val exclusionSet = config.excludePaths
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.trimStart('.').trim('/') }
            .toSet()

        if (System.getenv("CODECONTEXT_DEBUG_SCANNER") == "true") {
            println("[scanner-debug] root=${root.path}")
            println("[scanner-debug] config=${config.excludePaths}")
            println("[scanner-debug] exclusions=$exclusionSet")
        }

        return root.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                val name = file.name
                val relativePath = file.relativeTo(root).invariantSeparatorsPath
                val segments = relativePath.split('/').filter { it.isNotEmpty() }

                val matchesSupportedExtension =
                    name.endsWith(".kt") || name.endsWith(".java")

                val excludedByConfig = segments.any { segment ->
                    val normalized = segment.trim().trimStart('.').trim('/')
                    normalized.isNotEmpty() && normalized in exclusionSet
                }

                if (System.getenv("CODECONTEXT_DEBUG_SCANNER") == "true" && matchesSupportedExtension) {
                    println("[scanner-debug] candidate=$relativePath excluded=$excludedByConfig segments=$segments")
                }

                matchesSupportedExtension && !excludedByConfig
            }
            .toList()
    }
}
