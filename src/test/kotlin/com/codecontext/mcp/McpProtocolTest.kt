package com.codecontext.mcp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class McpProtocolTest {
    @Test
    fun initializeReturnsProtocolAndToolCapabilities() {
        val response = McpProtocol.handle(buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0")); put("id", JsonPrimitive(1)); put("method", JsonPrimitive("initialize")); put("params", buildJsonObject { put("protocolVersion", JsonPrimitive("2025-06-18")) })
        })
        assertEquals("2.0", response["jsonrpc"]?.toString()?.trim('"'))
        assertEquals("1", response["id"]?.toString())
        val result = response["result"].toString()
        assertTrue(result.contains("tools")); assertTrue(result.contains("CodeContext"))
    }

    @Test
    fun toolsListExposesEngineeringContextGateway() {
        val response = McpProtocol.handle(buildJsonObject { put("jsonrpc", JsonPrimitive("2.0")); put("id", JsonPrimitive(2)); put("method", JsonPrimitive("tools/list")) })
        val result = response["result"].toString()
        listOf("codecontext_analyze_repository", "codecontext_impact_analysis", "codecontext_architecture_analysis", "codecontext_pr_intelligence", "codecontext_get_engineering_reality", "codecontext_get_context_snapshot", "codecontext_get_context_diff", "codecontext_get_architecture_drift", "codecontext_get_architecture_contract", "codecontext_prepare_change", "codecontext_get_change_contract", "codecontext_get_evidence", "codecontext_change_safety", "codecontext_verify_change").forEach { assertTrue(result.contains(it), "Missing MCP tool: $it") }
    }

    @Test
    fun unknownMethodReturnsJsonRpcMethodNotFoundError() {
        val response = McpProtocol.handle(buildJsonObject { put("jsonrpc", JsonPrimitive("2.0")); put("id", JsonPrimitive(3)); put("method", JsonPrimitive("does/not/exist")) })
        assertEquals("-32601", response["error"]?.let { it.toString().substringAfter("\"code\":").substringBefore(',').trim() })
    }

    @Test
    fun remoteRepositoryArgumentsAreRejected() {
        val response = McpProtocol.handle(buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0")); put("id", JsonPrimitive(4)); put("method", JsonPrimitive("tools/call"))
            put("params", buildJsonObject { put("name", JsonPrimitive("codecontext_get_engineering_reality")); put("arguments", buildJsonObject { put("repoPath", JsonPrimitive("https://github.com/spring-projects/spring-petclinic")) }) })
        })
        assertTrue(response.toString().contains("Remote repositories are not supported"))
    }
}
