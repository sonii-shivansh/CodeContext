package com.codecontext.cli

import com.codecontext.core.cache.CacheManager
import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.intelligence.EngineeringContextEngine
import com.codecontext.core.intelligence.EngineeringContextSnapshot
import com.codecontext.core.scanner.RepositoryScanner
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import kotlinx.serialization.json.Json

class EngineeringContextSnapshotCommand : CliktCommand(name = "context-snapshot", help = "Create a deterministic repository engineering-context snapshot") {
    private val path by argument("path", help = "Repository path")
    private val jsonOutput by option("--json", help = "Write machine-readable snapshot JSON").flag()

    override fun run() {
        val root = File(path).absoluteFile.normalize()
        val snapshot = EngineeringContextEngine.snapshot(root, RepositoryScanner(ConfigLoader.load()))
        val output = File("output/engineering-context.json")
        if (jsonOutput) {
            output.parentFile.mkdirs()
            output.writeText(EngineeringContextEngine.encode(snapshot))
            echo("🧭 Engineering context: ${output.absolutePath}")
        }
        echo("🧭 Engineering Context")
        echo("├─ Commit: ${snapshot.repositoryCommit ?: "unknown"}")
        echo("├─ Files: ${snapshot.totalFiles}")
        echo("├─ Bytes: ${snapshot.totalBytes}")
        echo("├─ Dirty: ${snapshot.dirty}")
        echo("└─ Digest: ${snapshot.snapshotDigest}")
    }
}

class EngineeringContextDiffCommand : CliktCommand(name = "context-diff", help = "Compare two engineering-context snapshots") {
    private val before by argument("before", help = "Baseline snapshot JSON")
    private val after by argument("after", help = "Current snapshot JSON")
    private val jsonOutput by option("--json", help = "Write machine-readable diff JSON").flag()

    override fun run() {
        val json = Json { ignoreUnknownKeys = false }
        val baseline = json.decodeFromString(EngineeringContextSnapshot.serializer(), File(before).readText())
        val current = json.decodeFromString(EngineeringContextSnapshot.serializer(), File(after).readText())
        val diff = EngineeringContextEngine.diff(baseline, current)
        if (jsonOutput) {
            val output = File("output/engineering-context-diff.json")
            output.parentFile.mkdirs()
            output.writeText(EngineeringContextEngine.encode(diff))
            echo("🧭 Context diff: ${output.absolutePath}")
        }
        echo("🧭 Engineering Context Diff")
        echo("├─ Added: ${diff.summary.added}")
        echo("├─ Removed: ${diff.summary.removed}")
        echo("├─ Modified: ${diff.summary.modified}")
        echo("└─ Unchanged: ${diff.summary.unchanged}")
        diff.changes.take(30).forEach { change -> echo("   ${change.type}: ${change.path}") }
    }
}
