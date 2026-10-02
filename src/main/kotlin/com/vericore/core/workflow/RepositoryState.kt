package com.vericore.core.workflow

import java.io.File

/** Immutable repository identity captured for prepare/verify binding. */
object RepositoryState {
    fun canonicalPath(repoPath: String): String = File(repoPath).canonicalPath

    fun head(repoPath: String): String? {
        val root = File(repoPath).canonicalFile
        if (!File(root, ".git").exists()) return null
        return runCatching {
            ProcessBuilder("git", "rev-parse", "HEAD")
                .directory(root)
                .redirectErrorStream(true)
                .start()
                .apply { waitFor() }
                .inputStream.bufferedReader().readText().trim()
                .takeIf { it.matches(Regex("[0-9a-fA-F]{40}")) }
        }.getOrNull()
    }
}
