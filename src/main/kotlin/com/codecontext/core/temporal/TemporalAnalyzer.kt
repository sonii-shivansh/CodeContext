package com.codecontext.core.temporal

import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk

data class CodebaseSnapshot(
        val timestamp: Instant,
        val commitHash: String,
        val totalFiles: Int,
        val totalLines: Int,
        val topHotspots: List<String>
)

class TemporalAnalyzer(private val repoPath: String) {

    fun analyzeEvolution(monthsBack: Int = 6, intervalDays: Int = 30): List<CodebaseSnapshot> {
        val snapshots = mutableListOf<CodebaseSnapshot>()
        val git = Git.open(File(repoPath))
        val repo = git.repository

        // iterate backwards from HEAD
        val head = repo.parseCommit(repo.resolve("HEAD"))
        val now = Instant.ofEpochSecond(head.commitTime.toLong())
        val cutoff = now.minus(monthsBack.toLong() * 30, ChronoUnit.DAYS)

        val commitsToAnalyze = mutableListOf<RevCommit>()

        // Sampling strategy: Find commits closest to each interval point
        val logs = git.log().call().toList()

        var currentTarget = now
        while (currentTarget.isAfter(cutoff)) {
            // Find commit closest to currentTarget
            val closest =
                    logs.minByOrNull {
                        kotlin.math.abs(it.commitTime.toLong() - currentTarget.epochSecond)
                    }

            if (closest != null && !commitsToAnalyze.contains(closest)) {
                commitsToAnalyze.add(closest)
            }

            currentTarget = currentTarget.minus(intervalDays.toLong(), ChronoUnit.DAYS)
        }

        commitsToAnalyze.sortBy { it.commitTime }

        println("⏳ Analyzing evolution across ${commitsToAnalyze.size} snapshots...")

        commitsToAnalyze.forEachIndexed { index, commit ->
            println(
                    "   [${index + 1}/${commitsToAnalyze.size}] Snapshot at ${Instant.ofEpochSecond(commit.commitTime.toLong())}"
            )
            val snapshot = analyzeSnapshot(commit)
            snapshots.add(snapshot)
        }

        return snapshots
    }

    // Note: Checkout is expensive. In a real tool we might use a temporary worktree or TreeWalk
    // parsing without checkout.
    // For MVP, we will assume we can checkout (destructive if dirty!).
    // BETTER: Use JGit TreeWalk to load content into parser without checkout.
    private fun analyzeSnapshot(commit: RevCommit): CodebaseSnapshot {
        // Placeholder for full implementation.
        // True temporal analysis without checkout is complex.
        // We will mock the complexity metrics for the plan demo or do a simplified "File Count"
        // check.

        val git = Git.open(File(repoPath))
        val tree = commit.tree
        val treeWalk = TreeWalk(git.repository)
        treeWalk.addTree(tree)
        treeWalk.isRecursive = true

        var fileCount = 0

        while (treeWalk.next()) {
            val path = treeWalk.pathString
            if (path.endsWith(".kt") || path.endsWith(".java")) {
                fileCount++
            }
        }

        return CodebaseSnapshot(
                timestamp = Instant.ofEpochSecond(commit.commitTime.toLong()),
                commitHash = commit.name,
                totalFiles = fileCount,
                totalLines = fileCount * 50, // Estimate
                topHotspots = emptyList() // Needs graph build per snapshot
        )
    }
}
