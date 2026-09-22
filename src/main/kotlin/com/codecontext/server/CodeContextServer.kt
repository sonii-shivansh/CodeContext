package com.codecontext.server

import com.codecontext.cli.CodeParallelParser
import com.codecontext.core.ai.AICodeAnalyzer
import com.codecontext.core.ai.CodebaseContext
import com.codecontext.core.cache.CacheManager
import com.codecontext.core.config.CodeContextConfig
import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.graph.RobustDependencyGraph
import com.codecontext.core.scanner.OptimizedGitAnalyzer
import com.codecontext.core.scanner.RepositoryScanner
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.serialization.Serializable

@Serializable data class AnalysisRequest(val repoPath: String)
@Serializable data class AskRequest(val repoPath: String, val question: String)
@Serializable data class AnalysisResponse(val fileCount: Int, val hotspots: List<HotspotInfo>, val reportPath: String)
@Serializable data class HotspotInfo(val file: String, val score: Double)

fun Application.module() {
    install(ContentNegotiation) { json() }
    install(CORS) {
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader("x-api-key")
        anyHost()
    }
    configureRateLimiting()
    routing {
        staticFiles("/reports", File("output"))
        get("/") { call.respondText("CodeContext API is running. 🚀") }
        get("/health") { call.respond(mapOf("status" to "healthy", "version" to "0.1.0", "uptime" to System.currentTimeMillis() / 1000)) }
        get("/health/live") { call.respond(mapOf("status" to "live")) }
        get("/health/ready") { call.respond(mapOf("status" to "ready")) }

        post("/analyze") {
            try {
                val request = call.receive<AnalysisRequest>()
                var path = request.repoPath
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to "Remote repositories are not supported by this local endpoint"))
                    return@post
                }
                val sanitizedPath = sanitizePath(path)
                    ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to "Invalid or unsafe repository path"))
                val config = ConfigLoader.load()
                val (graph, parsedFiles, _) = AnalysisLogic.analyze(sanitizedPath, config)
                val enrichedFiles = OptimizedGitAnalyzer().analyze(sanitizedPath, parsedFiles)
                val reportFile = File("output/${File(sanitizedPath).name}-report.html").apply { parentFile.mkdirs() }
                com.codecontext.output.ReportGenerator().generate(graph, reportFile.absolutePath, enrichedFiles, com.codecontext.core.generator.LearningPathGenerator().generate(graph))
                val hotspots = graph.getTopHotspots(5).map { HotspotInfo(File(it.first).name, it.second) }
                call.respond(AnalysisResponse(parsedFiles.size, hotspots, reportFile.absolutePath))
            } catch (e: IllegalArgumentException) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            } catch (e: Exception) {
                call.respond(io.ktor.http.HttpStatusCode.InternalServerError, mapOf("error" to "Analysis failed"))
            }
        }

        post("/ask") {
            try {
                val request = call.receive<AskRequest>()
                require(request.question.isNotBlank() && request.question.length <= 16_000) { "Question is invalid" }
                val config = ConfigLoader.load()
                val sanitizedPath = sanitizePath(request.repoPath)
                    ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to "Invalid or unsafe repository path"))
                if (!config.ai.enabled) return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to "AI disabled in config"))
                val (graph, parsedFiles, _) = AnalysisLogic.analyze(sanitizedPath, config)
                val context = CodebaseContext(parsedFiles.size, listOf("Kotlin/Java"), graph.getTopHotspots(10).map { it.first }, emptyList())
                call.respond(AICodeAnalyzer(config.ai.apiKey, config.ai.model, config.ai.provider).askQuestion(request.question, context))
            } catch (e: IllegalArgumentException) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            } catch (e: Exception) {
                call.respond(io.ktor.http.HttpStatusCode.BadGateway, mapOf("error" to "AI provider request failed"))
            }
        }

        post("/analyze-org") {
            try {
                val paths = call.receive<List<String>>()
                require(paths.isNotEmpty() && paths.size <= 20) { "At most 20 repositories may be analyzed per request" }
                paths.forEach { require(sanitizePath(it) != null) { "Invalid or unsafe repository path" } }
                call.respond(com.codecontext.enterprise.OrganizationAnalyzer().analyzeRepositories(paths))
            } catch (e: IllegalArgumentException) {
                call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            } catch (e: Exception) {
                call.respond(io.ktor.http.HttpStatusCode.InternalServerError, mapOf("error" to "Organization analysis failed"))
            }
        }
    }
}

object AnalysisLogic {
    suspend fun analyze(repoPath: String, config: CodeContextConfig = ConfigLoader.load()): Triple<RobustDependencyGraph, List<com.codecontext.core.parser.ParsedFile>, CacheManager> {
        val files = RepositoryScanner(config).scan(repoPath)
        require(files.size <= config.maxFilesAnalyze) { "Repository exceeds the maximum file limit: ${config.maxFilesAnalyze}" }
        val cacheManager = CacheManager()
        val parsedFiles = CodeParallelParser(cacheManager).parseFiles(files)
        val graph = RobustDependencyGraph()
        graph.build(parsedFiles)
        graph.analyze()
        return Triple(graph, parsedFiles, cacheManager)
    }
}

/** Resolves a readable directory under explicitly configured roots. */
fun sanitizePath(inputPath: String): String? {
    return try {
        if (inputPath.isBlank() || inputPath.length > 4096) return null
        val candidate = Paths.get(inputPath).toRealPath()
        if (!Files.isDirectory(candidate) || !Files.isReadable(candidate)) return null
        val configured = System.getenv("CODECONTEXT_ALLOWED_PATHS")
        val roots = (configured?.split(File.pathSeparator)?.filter { it.isNotBlank() }
            ?: listOf(System.getProperty("user.dir"), System.getProperty("java.io.tmpdir")))
            .mapNotNull { runCatching { Paths.get(it).toRealPath() }.getOrNull() }
        if (roots.any { root -> candidate == root || candidate.startsWith(root) }) candidate.toString() else null
    } catch (_: Exception) {
        null
    }
}

fun Application.configureRateLimiting() {
    val config = ConfigLoader.load()
    if (!config.rateLimit.enabled) return
    val rateLimiter = RateLimiter(config.rateLimit.requestsPerMinute, config.rateLimit.requestsPerHour)
    intercept(ApplicationCallPipeline.Call) {
        val clientId = call.request.header("x-api-key")?.let { "key:${it.hashCode()}" } ?: "ip:${call.request.local.remoteHost}"
        if (!rateLimiter.checkLimit(clientId)) {
            val retryAfter = rateLimiter.getSecondsUntilReset(clientId)
            call.response.headers.append("Retry-After", retryAfter.toString())
            call.respond(io.ktor.http.HttpStatusCode.TooManyRequests, mapOf("error" to "Rate limit exceeded", "retryAfter" to retryAfter))
            return@intercept finish()
        }
        call.response.headers.append("X-RateLimit-Limit", config.rateLimit.requestsPerMinute.toString())
        call.response.headers.append("X-RateLimit-Remaining", rateLimiter.getRemainingMinute(clientId).toString())
        proceed()
    }
}
