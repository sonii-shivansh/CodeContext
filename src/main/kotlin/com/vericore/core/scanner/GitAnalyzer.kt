package com.vericore.core.scanner

import com.vericore.core.parser.GitMetadata
import com.vericore.core.parser.ParsedFile
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

class GitAnalyzer {
    fun analyze(repoPath: String, files: List<ParsedFile>): List<ParsedFile> {
        val gitDir = File(repoPath, ".git")
        if (!gitDir.exists()) {
            println("⚠️ No .git directory found at $repoPath. Skipping Git analysis.")
            return files
        }

        try {
            val repository: Repository =
                    FileRepositoryBuilder().setGitDir(gitDir).readEnvironment().findGitDir().build()

            val git = Git(repository)
            val commits = git.log().call().toList()

            // Map file path -> metadata builder
            val fileStats = mutableMapOf<String, FileStats>()

            commits.forEach { commit ->
                // For a real robust implementation, we'd use TreeWalk to see exactly which files
                // changed in each commit.
                // For MVP efficiency on large repos, checking commit messages or using a simplified
                // diff is faster but less accurate.
                // Let's do it properly: TreeWalk for each commit is expensive for history.
                // ALTERNATIVE: 'git log --name-only' equivalent.
                // JGit's TreeWalk is the way.

                // Simplified approach for MVP Phase 2:
                // We will iterate over the FILES and run `git log <file>` for each.
                // This is clearer logic-wise, though maybe slower for huge repos.
                // Given we have ~1000 files max for typical target, it's fine.
            }

            // OPTIMIZED APPROACH:
            // Iterate files, ask specific log for that file path.
            val result =
                    files.map { parsed ->
                        val relativePath = getRelativePath(File(repoPath), parsed.file)
                        try {
                            // Note: JGit log().addPath() filters commits for that file
                            val fileLog = git.log().addPath(relativePath).call().toList()

                            val changeFreq = fileLog.size
                            val lastMod =
                                    if (fileLog.isNotEmpty())
                                            fileLog.first().commitTime.toLong() * 1000
                                    else 0L

                            val authors =
                                    fileLog
                                            .groupBy { it.authorIdent.name }
                                            .mapValues { it.value.size }
                                            .entries
                                            .sortedByDescending { it.value }
                                            .take(3)
                                            .map { it.key }

                            val messages = fileLog.take(3).map { it.shortMessage }

                            parsed.copy(
                                    gitMetadata =
                                            GitMetadata(lastMod, changeFreq, authors, messages)
                            )
                        } catch (e: Exception) {
                            // If file not tracked or error
                            parsed
                        }
                    }

            repository.close()
            return result
        } catch (e: Exception) {
            System.err.println("Failed to open git repo: ${e.message}")
            return files
        }
    }

    private fun getRelativePath(base: File, file: File): String {
        return file.absolutePath.substring(base.absolutePath.length + 1).replace("\\", "/")
    }

    // Helper class if we used the other approach
    private data class FileStats(var count: Int = 0)
}
