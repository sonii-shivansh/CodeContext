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
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.nio.file.Paths
import kotlinx.serialization.Serializable

@Serializable data class AnalysisRequest(val repoPath: String)

@Serializable data class AskRequest(val repoPath: String, val question: String)

@Serializable
data class AnalysisResponse(
        val fileCount: Int,
        val hotspots: List<HotspotInfo>,
        val reportPath: String
)

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

    // Configure rate limiting
    configureRateLimiting()

    routing {
        // Serve static reports
        staticFiles("/reports", File("output"))

        get("/") { call.respondText("CodeContext API is running. 🚀") }

        post("/analyze") {
            try {
                val request = call.receive<AnalysisRequest>()
                val apiKey = call.request.header("x-api-key")
                val tier = com.codecontext.enterprise.LicenseManager.getTier(apiKey)

                if (tier == com.codecontext.enterprise.LicenseManager.Tier.FREE) {
                    println("⚠️ Free tier request")
                }

                var path = request.repoPath

                // Check if it's a remote URL
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    println("🌍 Cloning remote repository: $path")
                    try {
                        val repoName = path.split("/").last().replace(".git", "")
                        val cloneDir = File("temp_repos", repoName)

                        // Delete if exists to get fresh copy (simple caching strategy)
                        if (cloneDir.exists()) {
                            cloneDir.deleteRecursively()
                        }

                        org.eclipse.jgit.api.Git.cloneRepository()
                                .setURI(path)
                                .setDirectory(cloneDir)
                                .call()

                        println("✅ Cloned to: ${cloneDir.absolutePath}")
                        path = cloneDir.absolutePath
                    } catch (e: Exception) {
                        call.respond(
                                io.ktor.http.HttpStatusCode.BadRequest,
                                mapOf("error" to "Failed to clone repo: ${e.message}")
                        )
                        return@post
                    }
                }

                // FIX: Validate and sanitize path
                val sanitizedPath = sanitizePath(path)
                if (sanitizedPath == null) {
                    call.respond(
                            io.ktor.http.HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid or unsafe repository path")
                    )
                    return@post
                }

                path = sanitizedPath

                // Security: Check if path exists and is readable
                val repoDir = File(path)
                if (!repoDir.exists() || !repoDir.canRead()) {
                    call.respond(
                            io.ktor.http.HttpStatusCode.BadRequest,
                            mapOf("error" to "Repository path does not exist or is not readable")
                    )
                    return@post
                }

                // Security check needed in real app to prevent scanning root
                // Using Refactored Logic
                val (graph, parsedFiles, _) = AnalysisLogic.analyze(path)

                // Git Analysis
                val gitAnalyzer = OptimizedGitAnalyzer()
                val enrichedFiles = gitAnalyzer.analyze(File(path).absolutePath, parsedFiles)

                // Generate Report (Headless)
                val reportFile = File("output/${File(path).name}-report.html")
                reportFile.parentFile.mkdirs()
                com.codecontext.output.ReportGenerator()
                        .generate(
                                graph,
                                reportFile.absolutePath,
                                enrichedFiles,
                                com.codecontext.core.generator.LearningPathGenerator()
                                        .generate(graph)
                        )

                val hotspots = graph.getTopHotspots(5).map { HotspotInfo(it.first, it.second) }

                // Return URL relative to server static path if needed, or absolute path for local
                // demo
                call.respond(AnalysisResponse(parsedFiles.size, hotspots, reportFile.absolutePath))
            } catch (e: Exception) {
                call.respond(
                        io.ktor.http.HttpStatusCode.InternalServerError,
                        mapOf("error" to e.message)
                )
            }
        }

        post("/ask") {
            try {
                val request = call.receive<AskRequest>()
                val config = ConfigLoader.load()

                // FIX: Validate path for ask endpoint too
                val sanitizedPath = sanitizePath(request.repoPath)
                if (sanitizedPath == null) {
                    call.respond(
                            io.ktor.http.HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid or unsafe repository path")
                    )
                    return@post
                }

                if (!config.ai.enabled) {
                    call.respond(
                            io.ktor.http.HttpStatusCode.BadRequest,
                            mapOf("error" to "AI disabled in config")
                    )
                    return@post
                }

                val (graph, parsedFiles, _) = AnalysisLogic.analyze(sanitizedPath)

                val hotspots = graph.getTopHotspots(10).map { it.first }
                val context =
                        CodebaseContext(
                                totalFiles = parsedFiles.size,
                                languages = listOf("Kotlin/Java"),
                                hotspots = hotspots,
                                recentChanges = emptyList()
                        )

                val aiAnalyzer =
                        AICodeAnalyzer(config.ai.apiKey, config.ai.model, config.ai.provider)
                val response = aiAnalyzer.askQuestion(request.question, context)

                call.respond(response)
            } catch (e: Exception) {
                call.respond(
                        io.ktor.http.HttpStatusCode.InternalServerError,
                        mapOf("error" to e.message)
                )
            }
        }

        post("/analyze-org") {
            try {
                val paths = call.receive<List<String>>()
                val analyzer = com.codecontext.enterprise.OrganizationAnalyzer()

                val results = analyzer.analyzeRepositories(paths)

                call.respond(results)
            } catch (e: Exception) {
                call.respond(
                        io.ktor.http.HttpStatusCode.InternalServerError,
                        mapOf("error" to e.message)
                )
            }
        }
    }
}

// Deep Dive Improvement: Extracted logic to ensure consistency and testability
object AnalysisLogic {
    suspend fun analyze(
            repoPath: String
    ): Triple<
            RobustDependencyGraph,
            List<com.codecontext.core.parser.ParsedFile>,
            com.codecontext.core.cache.CacheManager> {
        val files = RepositoryScanner().scan(repoPath)
        val cacheManager = CacheManager()
        val parser = CodeParallelParser(cacheManager)
        val parsedFiles = parser.parseFiles(files)

        val graph = RobustDependencyGraph()
        graph.build(parsedFiles)
        graph.analyze()

        return Triple(graph, parsedFiles, cacheManager)
    }
}

// FIX: Security helper function to prevent path traversal
fun sanitizePath(inputPath: String): String? {
    try {
        // Remove any dangerous characters
        if (inputPath.contains("..") || inputPath.contains("~")) {
            return null
        }

        // Convert to absolute path and verify it's within allowed directories
        val path = Paths.get(inputPath).toAbsolutePath().normalize()
        val canonicalPath = path.toFile().canonicalPath

        // Define allowed base directories (configure these for your deployment)
        val allowedBases =
                listOf(
                        "/tmp/codecontext",
                        "/home",
                        "/Users",
                        "/workspace",
                        "C:\\Users",
                        "C:\\workspace",
                        "C:\\temp",
                        System.getProperty("user.dir"), // Current working directory
                        System.getProperty("user.home") // User home directory
                )

        // Check if path starts with any allowed base
        val isAllowed =
                allowedBases.any { base ->
                    try {
                        canonicalPath.startsWith(File(base).canonicalPath)
                    } catch (e: Exception) {
                        false
                    }
                }

        return if (isAllowed) canonicalPath else null
    } catch (e: Exception) {
        return null
    }
}

/**
 * Configure rate limiting for the API server. Intercepts requests and enforces per-client rate
 * limits.
 */
fun Application.configureRateLimiting() {
    val config = ConfigLoader.load()

    // Skip if rate limiting is disabled
    if (!config.rateLimit.enabled) {
        println("⚠️  Rate limiting is disabled")
        return
    }

    val rateLimiter =
            RateLimiter(
                    maxRequestsPerMinute = config.rateLimit.requestsPerMinute,
                    maxRequestsPerHour = config.rateLimit.requestsPerHour
            )

    println(
            "✅ Rate limiting enabled: ${config.rateLimit.requestsPerMinute}/min, ${config.rateLimit.requestsPerHour}/hour"
    )

    intercept(ApplicationCallPipeline.Call) {
        // Extract client identifier (API key or IP address)
        val clientId = call.request.header("x-api-key") ?: call.request.local.remoteHost

        // Check rate limit
        if (!rateLimiter.checkLimit(clientId)) {
            val retryAfter = rateLimiter.getSecondsUntilReset(clientId)
            val remaining = rateLimiter.getRemainingMinute(clientId)

            call.response.headers.append("Retry-After", retryAfter.toString())
            call.response.headers.append(
                    "X-RateLimit-Limit",
                    config.rateLimit.requestsPerMinute.toString()
            )
            call.response.headers.append("X-RateLimit-Remaining", "0")
            call.response.headers.append(
                    "X-RateLimit-Reset",
                    (System.currentTimeMillis() / 1000 + retryAfter).toString()
            )

            call.respond(
                    io.ktor.http.HttpStatusCode.TooManyRequests,
                    mapOf(
                            "error" to "Rate limit exceeded",
                            "message" to
                                    "Too many requests. Please try again in $retryAfter seconds.",
                            "retryAfter" to retryAfter
                    )
            )
            return@intercept finish()
        }

        // Add rate limit headers to successful responses
        val remaining = rateLimiter.getRemainingMinute(clientId)
        call.response.headers.append(
                "X-RateLimit-Limit",
                config.rateLimit.requestsPerMinute.toString()
        )
        call.response.headers.append("X-RateLimit-Remaining", remaining.toString())

        proceed()
    }
}
