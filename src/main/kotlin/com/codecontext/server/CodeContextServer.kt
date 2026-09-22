package com.codecontext.server

import com.codecontext.cli.CodeParallelParser
import com.codecontext.core.ai.AICodeAnalyzer
import com.codecontext.core.ai.CodebaseContext
import com.codecontext.core.cache.CacheManager
import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.scanner.OptimizedGitAnalyzer
import com.codecontext.core.scanner.RepositoryScanner
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable data class AnalysisRequest(val repoPath: String)
@Serializable data class AskRequest(val repoPath: String, val question: String)
@Serializable data class AnalysisResponse(val fileCount: Int, val hotspots: List<HotspotInfo>, val reportUrl: String)
@Serializable data class HotspotInfo(val file: String, val score: Double)
@Serializable data class ApiError(val error: String)

private const val MAX_REMOTE_URL_LENGTH = 2048
private const val MAX_QUESTION_LENGTH = 16_000

fun Application.module() {
    install(ContentNegotiation) { json() }
    // CORS is intentionally not enabled by default. Configure a trusted reverse proxy or
    // add an explicit allowlist before exposing this local service to a browser application.
    configureRateLimiting()

    routing {
        staticFiles("/reports", File("output"))
        get("/") { call.respondText("CodeContext API is running. 🚀") }
        get("/health") { call.respond(mapOf("status" to "healthy", "version" to "0.2.0")) }
        get("/health/live") { call.respond(mapOf("status" to "live")) }
        get("/health/ready") { call.respond(mapOf("status" to "ready")) }

        post("/analyze") {
            var temporaryRepo: Path? = null
            try {
                val request = call.receive<AnalysisRequest>()
                require(request.repoPath.isNotBlank() && request.repoPath.length <= MAX_REMOTE_URL_LENGTH) {
                    "Repository path is invalid"
                }
                var path = request.repoPath
                if (path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)) {
                    temporaryRepo = cloneAllowedRepository(path)
                    path = temporaryRepo.toString()
                }
                val sanitizedPath = sanitizePath(path)
                    ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest, ApiError("Invalid or unsafe repository path"))
                val repoDir = File(sanitizedPath)
                if (!repoDir.isDirectory || !repoDir.canRead()) {
                    return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest, ApiError("Repository path does not exist or is not readable"))
                }

                val (graph, parsedFiles, _) = AnalysisLogic.analyze(sanitizedPath)
                val enrichedFiles = OptimizedGitAnalyzer().analyze(sanitizedPath, parsedFiles)
                val reportId = UUID.randomUUID().toString()
                val reportFile = File("output/$reportId.html").apply { parentFile.mkdirs() }
                com.codecontext.output.ReportGenerator().generate(
                    graph, reportFile.absolutePath, enrichedFiles,
                    com.codecontext.core.generator.LearningPathGenerator().generate(graph)
                )
                val hotspots = graph.getTopHotspots(5).map { HotspotInfo(File(it.first).name, it.second) }
                call.respond(AnalysisResponse(parsedFiles.size, hotspots, "/reports/$reportId.html"))
            } catch (e: IllegalArgumentException) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, ApiError(e.message ?: "Invalid request"))
            } catch (e: Exception) {
                System.err.println("Analysis failed: ${e::class.simpleName}")
                call.respond(io.ktor.http.HttpStatusCode.InternalServerError, ApiError("Analysis failed"))
            } finally {
                temporaryRepo?.toFile()?.deleteRecursively()
            }
        }

        post("/ask") {
            try {
                val request = call.receive<AskRequest>()
                require(request.question.isNotBlank() && request.question.length <= MAX_QUESTION_LENGTH) {
                    "Question is invalid"
                }
                val config = ConfigLoader.load()
                val sanitizedPath = sanitizePath(request.repoPath)
                    ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest, ApiError("Invalid or unsafe repository path"))
                if (!config.ai.enabled) {
                    return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest, ApiError("AI disabled in config"))
                }
                val (graph, parsedFiles, _) = AnalysisLogic.analyze(sanitizedPath)
                val context = CodebaseContext(parsedFiles.size, listOf("Kotlin/Java"), graph.getTopHotspots(10).map { it.first }, emptyList())
                call.respond(AICodeAnalyzer(config.ai.apiKey, config.ai.model, config.ai.provider).askQuestion(request.question, context))
            } catch (e: IllegalArgumentException) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, ApiError(e.message ?: "Invalid request"))
            } catch (e: Exception) {
                System.err.println("AI request failed: ${e::class.simpleName}")
                call.respond(io.ktor.http.HttpStatusCode.BadGateway, ApiError("AI provider request failed"))
            }
        }

        post("/analyze-org") {
            try {
                val paths = call.receive<List<String>>()
                require(paths.isNotEmpty() && paths.size <= 20) { "At most 20 repositories may be analyzed per request" }
                paths.forEach { require(sanitizePath(it) != null) { "Invalid or unsafe repository path" } }
                call.respond(com.codecontext.enterprise.OrganizationAnalyzer().analyzeRepositories(paths))
            } catch (e: IllegalArgumentException) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, ApiError(e.message ?: "Invalid request"))
            } catch (e: Exception) {
                call.respond(io.ktor.http.HttpStatusCode.InternalServerError, ApiError("Organization analysis failed"))
            }
        }
    }
}

private fun cloneAllowedRepository(rawUrl: String): Path {
    val uri = try { URI(rawUrl) } catch (_: Exception) { throw IllegalArgumentException("Repository URL is invalid") }
    require(uri.scheme.equals("https", true)) { "Only HTTPS repository URLs are allowed" }
    val host = uri.host?.lowercase() ?: throw IllegalArgumentException("Repository URL has no host")
    val allowedHosts = System.getenv("CODECONTEXT_ALLOWED_GIT_HOSTS")?.split(',')?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }
        ?: listOf("github.com")
    require(host in allowedHosts) { "Repository host is not allowed" }
    require(uri.userInfo == null && uri.query == null && uri.fragment == null) { "Repository URL contains unsupported components" }
    val destination = Files.createTempDirectory("codecontext-repo-")
    try {
        org.eclipse.jgit.api.Git.cloneRepository().setURI(uri.toString()).setDirectory(destination.toFile()).call().use { }
        return destination
    } catch (e: Exception) {
        destination.toFile().deleteRecursively()
        throw IllegalArgumentException("Repository clone failed")
    }
}

object AnalysisLogic {
    suspend fun analyze(repoPath: String): Triple<RobustDependencyGraph, List<com.codecontext.core.parser.ParsedFile>, CacheManager> {
        val files = RepositoryScanner().scan(repoPath)
        require(files.size <= 10_000) { "Repository exceeds the maximum file limit" }
        val cacheManager = CacheManager()
        val parsedFiles = CodeParallelParser(cacheManager).parseFiles(files)
        val graph = RobustDependencyGraph()
        graph.build(parsedFiles)
        graph.analyze()
        return Triple(graph, parsedFiles, cacheManager)
    }
}

/** Resolves a directory and ensures it remains inside an explicitly allowed workspace. */
fun sanitizePath(inputPath: String): String? = try {
    if (inputPath.isBlank() || inputPath.length > 4096) return null
    val candidate = Paths.get(inputPath).toRealPath()
    if (!Files.isDirectory(candidate) || !Files.isReadable(candidate)) return null
    val allowedRoots = (System.getenv("CODECONTEXT_ALLOWED_PATHS")?.split(File.pathSeparator)
        ?: listOf(System.getProperty("user.dir"), System.getProperty("java.io.tmpdir")))
        .mapNotNull { root -> runCatching { Paths.get(root).toRealPath() }.getOrNull() }
    if (allowedRoots.any { candidate == it || candidate.startsWith(it) }) candidate.toString() else null
} catch (_: Exception) { null }

fun Application.configureRateLimiting() {
    val config = ConfigLoader.load()
    if (!config.rateLimit.enabled) return
    val limiter = RateLimiter(config.rateLimit.requestsPerMinute, config.rateLimit.requestsPerHour)
    intercept(ApplicationCallPipeline.Call) {
        val clientId = call.request.header("x-api-key")?.let { "key:${it.hashCode()}" } ?: "ip:${call.request.local.remoteHost}"
        if (!limiter.checkLimit(clientId)) {
            val retryAfter = limiter.getSecondsUntilReset(clientId)
            call.response.headers.append("Retry-After", retryAfter.toString())
            call.respond(io.ktor.http.HttpStatusCode.TooManyRequests, ApiError("Rate limit exceeded"))
            return@intercept finish()
        }
        call.response.headers.append("X-RateLimit-Limit", config.rateLimit.requestsPerMinute.toString())
        call.response.headers.append("X-RateLimit-Remaining", limiter.getRemainingMinute(clientId).toString())
        proceed()
    }
}
