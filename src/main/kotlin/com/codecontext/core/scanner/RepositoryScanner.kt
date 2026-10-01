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
            .map { it.trim().trim('/') }
            .filter { it.isNotEmpty() }
            .toSet()

        return root.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                val name = file.name
                val relativePath = file.relativeTo(root).invariantSeparatorsPath
                val segments = relativePath.split('/').filter { it.isNotEmpty() }

                val matchesSupportedExtension =
                    name.endsWith(".kt") || name.endsWith(".java")

                // CodeContext owns these root-level directories. They must not be
                // scanned even when the target repository has no configuration.
                val isToolGeneratedRootPath = segments.firstOrNull() in setOf(".codecontext", "output")

                val excludedByConfig = segments.any { segment ->
                    segment in exclusionSet ||
                        (segment.startsWith('.') && segment.trimStart('.') in exclusionSet)
                }

                matchesSupportedExtension && !isToolGeneratedRootPath && !excludedByConfig
            }
            .toList()
    }
}
