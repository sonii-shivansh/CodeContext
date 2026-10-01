package com.codecontext.cli

import com.codecontext.core.ai.GroundedEvidenceBuilder
import com.codecontext.core.cache.CacheManager
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.intelligence.AnalysisSnapshotBuilder
import com.codecontext.core.parser.ParsedFile
import com.codecontext.core.qa.RepositoryEvidenceRetriever
import com.codecontext.core.qa.RepositoryQuestionClassifier
import com.codecontext.core.scanner.OptimizedGitAnalyzer
import com.codecontext.core.scanner.RepositoryScanner
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RepositoryQACommand : CliktCommand(name = "repo-qa", help = "Retrieve grounded repository evidence for a developer question") {
    private val question by argument("question", help = "Question about the repository")
    private val path by option("--path", help = "Repository path").default(".")
    private val maxResults by option("--max-results", help = "Maximum evidence items").int().default(8)

    override fun run() {
        val root = File(path).canonicalFile
        require(root.isDirectory) { "Repository path is not a directory: $path" }
        require(maxResults in 1..32) { "--max-results must be between 1 and 32" }

        val parsedFiles: List<ParsedFile> = runBlocking {
            val files = RepositoryScanner().scan(root.path)
            CodeParallelParser(CacheManager()).parseFiles(files)
        }
        val enriched = try {
            OptimizedGitAnalyzer().analyze(root.path, parsedFiles)
        } catch (_: Exception) {
            parsedFiles
        }
        val graph = RobustDependencyGraph()
        require(graph.build(enriched).isSuccess) { "Failed to build dependency graph" }
        require(graph.analyze().isSuccess) { "Failed to analyze dependency graph" }

        val snapshot = AnalysisSnapshotBuilder.build(
            repositoryPath = root.path,
            parsedFiles = enriched,
            graph = graph.graph,
            pageRankScores = graph.pageRankScores,
            hasCycles = graph.hasCycles,
            parseFailures = 0
        )
        val grounded = GroundedEvidenceBuilder.fromSnapshot(snapshot)
        val parsedQuestion = RepositoryQuestionClassifier.classify(question)
        val result = RepositoryEvidenceRetriever().retrieve(parsedQuestion, grounded, maxResults)

        val json = Json { prettyPrint = true; encodeDefaults = true }
        echo(json.encodeToString(result))
    }
}
