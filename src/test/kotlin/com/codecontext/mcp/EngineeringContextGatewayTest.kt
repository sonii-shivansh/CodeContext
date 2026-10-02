package com.codecontext.mcp

import com.codecontext.core.intelligence.ArchitectureIntelligenceEngine
import com.codecontext.core.intelligence.ArchitectureIntelligenceResult
import com.codecontext.core.scanner.RepositoryScanner
import com.codecontext.core.config.CodeContextConfig
import com.codecontext.cli.CodeParallelParser
import com.codecontext.core.cache.CacheManager
import com.codecontext.core.graph.RobustDependencyGraph
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotNull

class EngineeringContextGatewayTest {
    @Test
    fun `architecture drift gateway serializes a deterministic result`() {
        val root = Files.createTempDirectory("gateway-drift").toFile()
        try {
            root.resolve("src/App.kt").apply { parentFile.mkdirs(); writeText("class App") }
            val config = CodeContextConfig(excludePaths = emptyList())
            val parsed = runBlocking { CodeParallelParser(CacheManager()).parseFiles(RepositoryScanner(config).scan(root.path)) }
            val graph = RobustDependencyGraph()
            graph.build(parsed).getOrThrow()
            graph.analyze().getOrThrow()
            val architecture = ArchitectureIntelligenceEngine.analyze(graph.graph, root, config.architecture)
            val baseline = Json { encodeDefaults = true }.encodeToJsonElement(ArchitectureIntelligenceResult.serializer(), architecture).jsonObject
            val result = EngineeringContextGateway.architectureDrift(root.path, baseline)
            assertNotNull(result["summary"] ?: result["changes"])
        } finally { root.deleteRecursively() }
    }
}
