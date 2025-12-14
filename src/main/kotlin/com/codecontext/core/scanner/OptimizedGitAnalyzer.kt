package com.codecontext.core.scanner

import com.codecontext.core.parser.GitMetadata
import com.codecontext.core.parser.ParsedFile
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.TreeWalk

class OptimizedGitAnalyzer {
    fun analyze(repoPath: String, files: List<ParsedFile>): List<ParsedFile> {
        val gitDir = File(repoPath, ".git")
        if (!gitDir.exists()) {
            println("⚠️ No .git directory found. Skipping Git analysis.")
            // Return original files if no git
            return files
        }

        try {
            val repository: Repository =
                    FileRepositoryBuilder().setGitDir(gitDir).readEnvironment().findGitDir().build()

            val git = Git(repository)

            // OPTIMIZATION: Single pass through all commits
            val fileStats = mutableMapOf<String, FileChangeStats>()
            // Limit to recent 1000 commits for performance vs depth trade-off
            val commits = git.log().call().take(1000).toList()

            println("🔍 Analyzing ${commits.size} commits...")

            commits.forEachIndexed { index, commit ->
                if (index % 100 == 0 && index > 0) println("   Progress: $index/${commits.size}")

                // Get parent to compare changes
                val parent = if (commit.parentCount > 0) commit.getParent(0) else null

                if (parent != null) {
                    val oldTree = parent.tree
                    val newTree = commit.tree

                    // Use TreeWalk to efficiently compare trees
                    val diffs =
                            git.diff()
                                    .setOldTree(prepareTreeParser(repository, oldTree))
                                    .setNewTree(prepareTreeParser(repository, newTree))
                                    .call()

                    diffs.forEach { diff ->
                        // diff.newPath is usually the path unless deleted
                        val path =
                                if (diff.changeType == DiffEntry.ChangeType.DELETE) diff.oldPath
                                else diff.newPath
                        val stats = fileStats.getOrPut(path) { FileChangeStats() }

                        stats.changes++
                        stats.lastModified =
                                maxOf(stats.lastModified, commit.commitTime.toLong() * 1000)
                        stats.authors.add(commit.authorIdent.name)
                        stats.messages.add(commit.shortMessage)
                    }
                }
            }

            repository.close()

            // Map results back to files
            return files.map { parsed ->
                val relativePath = getRelativePath(File(repoPath), parsed.file)
                // Try direct match or forward slash match
                val stats = fileStats[relativePath] ?: fileStats[relativePath.replace("\\", "/")]

                if (stats != null) {
                    val topAuthors =
                            stats.authors
                                    .groupBy { it }
                                    .mapValues { it.value.size }
                                    .entries
                                    .sortedByDescending { it.value }
                                    .take(3)
                                    .map { it.key }

                    parsed.copy(
                            gitMetadata =
                                    GitMetadata(
                                            lastModified = stats.lastModified,
                                            changeFrequency = stats.changes,
                                            topAuthors = topAuthors,
                                            recentMessages = stats.messages.take(3)
                                    )
                    )
                } else {
                    parsed
                }
            }
        } catch (e: Exception) {
            System.err.println("Git analysis failed: ${e.message}")
            // Return original files on error
            return files
        }
    }

    // Helper to prepare tree parser for Diff
    private fun prepareTreeParser(
            repository: Repository,
            tree: org.eclipse.jgit.lib.ObjectId
    ): org.eclipse.jgit.treewalk.AbstractTreeIterator {
        val treeWalk = TreeWalk(repository)
        treeWalk.addTree(tree)
        treeWalk.isRecursive = true
        val parser = CanonicalTreeParser()
        parser.reset(repository.newObjectReader(), tree)
        return parser
    }

    private fun getRelativePath(base: File, file: File): String {
        return file.absolutePath
                .substring(base.absolutePath.length + 1)
                .replace("\\", "/") // Git uses forward slashes
    }

    private data class FileChangeStats(
            var changes: Int = 0,
            var lastModified: Long = 0,
            val authors: MutableSet<String> = mutableSetOf(),
            val messages: MutableList<String> = mutableListOf()
    )
}
