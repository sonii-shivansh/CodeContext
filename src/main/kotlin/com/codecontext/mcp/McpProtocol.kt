package com.codecontext.mcp

import com.codecontext.core.Version
import com.codecontext.core.config.ConfigLoader
import com.codecontext.core.intelligence.ArchitectureIntelligenceEngine
import com.codecontext.core.intelligence.ChangeImpactEngine
import com.codecontext.core.intelligence.GitChangeSetBuilder
import com.codecontext.core.intelligence.PRIntelligenceAnalyzer
import com.codecontext.core.scanner.OptimizedGitAnalyzer
import com.codecontext.server.AnalysisLogic
import com.codecontext.server.sanitizePath
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader

private val json = Json { encodeDefaults = true; explicitNulls = false }

/** MCP stdio transport for local CodeContext tooling. */
object McpProtocol {
    private const val PROTOCOL_VERSION = "2025-11-25"

    fun handle(request: JsonObject): JsonObject {
        val id = request["id"]
        val method = request["method"]?.jsonPrimitive?.content
            ?: return errorResponse(id, -32600, "Invalid Request")

        return when (method) {
            "initialize" -> initialize(id)
            "notifications/initialized" -> emptyResponse()
            "ping" -> resultResponse(id, buildJsonObject {})
            "tools/list" -> resultResponse(id, buildJsonObject { put("tools", toolDefinitions()) })
            "tools/call" -> callTool(id, request["params"]?.jsonObject ?: buildJsonObject {})
            else -> errorResponse(id, -32601, "Method not found: $method")
        }
    }

    fun runStdio(input: BufferedReader = BufferedReader(InputStreamReader(System.`in`))) {
        input.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val request = runCatching { json.parseToJsonElement(line).jsonObject }.getOrElse {
                println(json.encodeToString(JsonObject.serializer(), errorResponse(null, -32700, "Parse error")))
                return@forEachLine
            }
            val response = handle(request)
            if (response.isNotEmpty()) {
                println(json.encodeToString(JsonObject.serializer(), response))
                System.out.flush()
            }
        }
    }

    private fun initialize(id: JsonElement?): JsonObject = resultResponse(id, buildJsonObject {
        put("protocolVersion", JsonPrimitive(PROTOCOL_VERSION))
        put("capabilities", buildJsonObject { put("tools", buildJsonObject { put("listChanged", JsonPrimitive(false)) }) })
        put("serverInfo", buildJsonObject {
            put("name", JsonPrimitive("CodeContext"))
            put("version", JsonPrimitive(Version.current))
        })
        put("instructions", JsonPrimitive("CodeContext provides deterministic, evidence-backed engineering intelligence. Prefer these tools before modifying a repository."))
    })

    private fun callTool(id: JsonElement?, params: JsonObject): JsonObject {
        val name = params["name"]?.jsonPrimitive?.content
            ?: return errorResponse(id, -32602, "Missing tool name")
        val args = params["arguments"]?.jsonObject ?: buildJsonObject {}
        return try {
            resultResponse(id, when (name) {
                "codecontext_analyze_repository" -> analyzeRepository(args)
                "codecontext_impact_analysis" -> impactAnalysis(args)
                "codecontext_architecture_analysis" -> architectureAnalysis(args)
                "codecontext_pr_intelligence" -> prIntelligence(args)
                else -> return errorResponse(id, -32602, "Unknown tool: $name")
            })
        } catch (e: IllegalArgumentException) {
            errorResponse(id, -32602, e.message ?: "Invalid tool arguments")
        } catch (e: Exception) {
            System.err.println("MCP tool '$name' failed: ${e::class.simpleName}")
            errorResponse(id, -32603, "Tool execution failed")
        }
    }

    private fun analyzeRepository(args: JsonObject): JsonObject {
        val path = safeRepoPath(args)
        val config = ConfigLoader.load()
        val result = runBlocking { AnalysisLogic.analyze(path, config) }
        val graph = result.first
        val parsedFiles = result.second
        val hotspots = graph.getTopHotspots(10).map { (file, score) ->
            buildJsonObject {
                put("file", JsonPrimitive(file))
                put("score", JsonPrimitive(score))
            }
        }
        val payload = buildJsonObject {
            put("schemaVersion", JsonPrimitive("1.0"))
            put("repository", JsonPrimitive(path))
            put("fileCount", JsonPrimitive(parsedFiles.size))
            put("nodeCount", JsonPrimitive(graph.graph.vertexSet().size))
            put("edgeCount", JsonPrimitive(graph.graph.edgeSet().size))
            put("hotspots", JsonArray(hotspots))
        }
        return textResult(json.encodeToString(JsonObject.serializer(), payload))
    }

    private fun impactAnalysis(args: JsonObject): JsonObject {
        val path = safeRepoPath(args)
        val changedPaths = args["changedPaths"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        require(changedPaths.isNotEmpty()) { "changedPaths must contain at least one repository-relative path" }
        require(changedPaths.size <= 100) { "changedPaths may contain at most 100 paths" }
        val config = ConfigLoader.load()
        val (graph, parsedFiles, _) = runBlocking { AnalysisLogic.analyze(path, config) }
        val enrichedFiles = OptimizedGitAnalyzer().analyze(path, parsedFiles)
        val pathLookup = enrichedFiles.associateBy { it.file.absolutePath.replace('\\', '/') }
        val changedAbsolute = changedPaths.map { java.io.File(path, it).absolutePath.replace('\\', '/') }
        val result = ChangeImpactEngine.analyze(
            graph.graph,
            changedAbsolute,
            graph.pageRankScores,
            pathLookup.mapValues { it.value.gitMetadata.changeFrequency },
            pathLookup.mapValues { it.value.packageName }
        )
        return textResult(json.encodeToString(com.codecontext.core.intelligence.ChangeImpactResult.serializer(), result))
    }

    private fun architectureAnalysis(args: JsonObject): JsonObject {
        val path = safeRepoPath(args)
        val config = ConfigLoader.load()
        val (graph, _, _) = runBlocking { AnalysisLogic.analyze(path, config) }
        val result = ArchitectureIntelligenceEngine.analyze(graph.graph, java.io.File(path), config.architecture)
        return textResult(json.encodeToString(com.codecontext.core.intelligence.ArchitectureIntelligenceResult.serializer(), result))
    }

    private fun prIntelligence(args: JsonObject): JsonObject {
        val path = safeRepoPath(args)
        val base = args["baseRevision"]?.jsonPrimitive?.content
        val head = args["headRevision"]?.jsonPrimitive?.content
        require((base == null) == (head == null)) { "baseRevision and headRevision must be supplied together" }
        if (base != null) require(base.length <= 256 && head!!.length <= 256) { "Git revisions are too long" }
        val changeSet = if (base == null) {
            GitChangeSetBuilder.fromWorkingTree(path)
        } else {
            GitChangeSetBuilder.fromRevisions(path, base, head!!)
        }
        val result = runBlocking { PRIntelligenceAnalyzer.analyze(path, changeSet, ConfigLoader.load()) }
        return textResult(json.encodeToString(com.codecontext.core.intelligence.PRIntelligenceResult.serializer(), result))
    }

    private fun safeRepoPath(args: JsonObject): String {
        val input = args["repoPath"]?.jsonPrimitive?.content ?: error("repoPath is required")
        require(!input.startsWith("http://", true) && !input.startsWith("https://", true)) { "Remote repositories are not supported" }
        return sanitizePath(input) ?: error("Invalid or unsafe repository path")
    }

    private fun toolDefinitions(): JsonArray = buildJsonArray {
        add(tool("codecontext_analyze_repository", "Analyze a repository and return deterministic structure, graph, and hotspot evidence.", repositorySchema()))
        add(tool("codecontext_impact_analysis", "Calculate deterministic dependency impact for changed repository-relative paths.", buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("required", buildJsonArray { add(JsonPrimitive("repoPath")); add(JsonPrimitive("changedPaths")) })
            put("properties", buildJsonObject {
                put("repoPath", stringProperty("Absolute repository path"))
                put("changedPaths", buildJsonObject {
                    put("type", JsonPrimitive("array"))
                    put("items", stringProperty("Repository-relative changed path"))
                    put("maxItems", JsonPrimitive(100))
                })
            })
        }))
        add(tool("codecontext_architecture_analysis", "Analyze architecture boundaries, dependencies, and architectural signals.", repositorySchema()))
        add(tool("codecontext_pr_intelligence", "Analyze working-tree or revision-to-revision changes and return PR intelligence.", buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("required", buildJsonArray { add(JsonPrimitive("repoPath")) })
            put("properties", buildJsonObject {
                put("repoPath", stringProperty("Absolute repository path"))
                put("baseRevision", stringProperty("Optional Git base revision"))
                put("headRevision", stringProperty("Optional Git head revision"))
            })
        }))
    }

    private fun repositorySchema(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put("required", buildJsonArray { add(JsonPrimitive("repoPath")) })
        put("properties", buildJsonObject { put("repoPath", stringProperty("Absolute repository path")) })
    }

    private fun stringProperty(description: String): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("string"))
        put("description", JsonPrimitive(description))
    }

    private fun tool(name: String, description: String, inputSchema: JsonObject): JsonObject = buildJsonObject {
        put("name", JsonPrimitive(name))
        put("description", JsonPrimitive(description))
        put("inputSchema", inputSchema)
    }

    private fun textResult(text: String): JsonObject = buildJsonObject {
        put("content", buildJsonArray { add(buildJsonObject { put("type", JsonPrimitive("text")); put("text", JsonPrimitive(text)) }) })
        put("isError", JsonPrimitive(false))
    }

    private fun resultResponse(id: JsonElement?, result: JsonObject): JsonObject = buildJsonObject {
        put("jsonrpc", JsonPrimitive("2.0"))
        if (id != null) put("id", id)
        put("result", result)
    }

    private fun errorResponse(id: JsonElement?, code: Int, message: String): JsonObject = buildJsonObject {
        put("jsonrpc", JsonPrimitive("2.0"))
        if (id != null) put("id", id)
        put("error", buildJsonObject {
            put("code", JsonPrimitive(code))
            put("message", JsonPrimitive(message))
        })
    }

    private fun emptyResponse(): JsonObject = buildJsonObject {}
}
