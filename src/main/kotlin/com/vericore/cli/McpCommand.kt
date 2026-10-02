package com.vericore.cli

import com.vericore.mcp.McpProtocol
import com.github.ajalt.clikt.core.CliktCommand

/** Starts the local Model Context Protocol server over stdin/stdout. */
class McpCommand :
    CliktCommand(name = "mcp", help = "Start the Vericore MCP server over stdio") {
    override fun run() {
        McpProtocol.runStdio()
    }
}
