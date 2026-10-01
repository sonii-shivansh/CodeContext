package com.codecontext.mcp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class McpProtocolTest {
    @Test
    fun initializeReturnsProtocolAndToolCapabilities() {
        val response = McpProtocol.handle(
            buildJsonObject {
                put("jsonrpc", JsonPrimitive("2.0"))
                put("id", JsonPrimitive(1))
                put("method", JsonPrimitive("initialize"))
                put("params", buildJsonObject { put("protocolVersion", JsonPrimitive("2025-06-18")) })
            }
        )

        assertEquals("2.0", response["jsonrpc"]?.toString()?.trim('"'))
        assertEquals("1", response["id"]?.toString())
        val result = response["result"].toString()
        assertTrue(result.contains("tools"))
        assertTrue(result.contains("CodeContext"))
    }

    @Test
    fun toolsListExposesEngineeringIntelligenceTools() {
        val response = McpProtocol.handle(
            buildJsonObject {
                put("jsonrpc", JsonPrimitive("2.0"))
                put("id", JsonPrimitive(2))
                put("method", JsonPrimitive("tools/list"))
            }
        )

        val result = response["result"].toString()
        assertTrue(result.contains("codecontext_analyze_repository"))
        assertTrue(result.contains("codecontext_impact_analysis"))
        assertTrue(result.contains("codecontext_architecture_analysis"))
        assertTrue(result.contains("codecontext_pr_intelligence"))
    }

    @Test
    fun unknownMethodReturnsJsonRpcMethodNotFoundError() {
        val response = McpProtocol.handle(
            buildJsonObject {
                put("jsonrpc", JsonPrimitive("2.0"))
                put("id", JsonPrimitive(3))
                put("method", JsonPrimitive("does/not/exist"))
            }
        )

        assertEquals("-32601", response["error"]?.let { it.toString().substringAfter("\"code\":").substringBefore(',').trim() })
    }
}
