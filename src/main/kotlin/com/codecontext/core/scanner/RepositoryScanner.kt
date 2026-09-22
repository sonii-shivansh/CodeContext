package com.codecontext.core.scanner

import com.codecontext.core.config.CodeContextConfig
import com.codecontext.core.config.ConfigLoader
import java.io.File

class RepositoryScanner(
    private val config: CodeContextConfig = ConfigLoader.load()
) {
    fun scan(rootPath: String): List<File> {
        val root = File(rootPath)
        if (!root.exists() || !root.isDirectory) {
            throw IllegalArgumentException("Invalid repository path: $rootPath")
        }

        val exclusionSet = config.excludePaths
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.trimStart('.').trim('/') }
            .toSet()

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

                matchesSupportedExtension && !excludedByConfig
            }
            .toList()
    }
}
