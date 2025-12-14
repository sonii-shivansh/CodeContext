package com.codecontext.enterprise

import com.codecontext.cli.CodeParallelParser
import com.codecontext.core.cache.CacheManager
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.scanner.RepositoryScanner
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

data class RepoResult(
        val name: String,
        val fileCount: Int,
        val hotspots: List<Pair<String, Double>>,
        val error: String? = null
)

class OrganizationAnalyzer {

    suspend fun analyzeRepositories(repoPaths: List<String>): List<RepoResult> = coroutineScope {
        echo("🏢 Starting Organization Analysis for ${repoPaths.size} repositories...")

        repoPaths.map { path -> async { analyzeSingleRepo(path) } }.awaitAll()
    }

    private fun analyzeSingleRepo(path: String): RepoResult {
        return try {
            val file = File(path)
            if (!file.exists()) {
                return RepoResult(path, 0, emptyList(), "Path not found")
            }

            // 1. Scan
            val scanner = RepositoryScanner()
            val files = scanner.scan(path)

            if (files.isEmpty()) {
                return RepoResult(file.name, 0, emptyList(), "No source files")
            }

            // 2. Parse (Parallel)
            val cacheManager = CacheManager() // Separate cache per repo or shared? Shared is fine.
            val parser = CodeParallelParser(cacheManager)

            // Calling suspend function from blocking context if inside async?
            // analyzeRepositories is suspend, so we can call suspend functions.
            // But CodeParallelParser.parseFiles is suspend.
            // We need to match contexts.

            val parsedFiles = runBlocking {
                parser.parseFiles(files)
            } // Blocking inside the async thread

            // 3. Graph
            val graph = RobustDependencyGraph()
            graph.build(parsedFiles)
            graph.analyze()

            // 4. Hotspots
            val hotspots = graph.getTopHotspots(5)

            RepoResult(file.name, files.size, hotspots)
        } catch (e: Exception) {
            e.printStackTrace()
            RepoResult(File(path).name, 0, emptyList(), e.message)
        }
    }

    private fun echo(msg: String) {
        println(msg)
    }
}
