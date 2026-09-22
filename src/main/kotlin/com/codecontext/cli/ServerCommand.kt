package com.codecontext.cli

import com.codecontext.server.module
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/** Starts the local CodeContext HTTP API. */
class ServerCommand :
    CliktCommand(name = "server", help = "Start CodeContext as a local API server") {
    private val host by option("-h", "--host", help = "Host to bind to").default("127.0.0.1")
    private val port by option("-p", "--port", help = "Port to listen on").int().default(8080)

    override fun run() {
        require(port in 1..65535) { "Port must be between 1 and 65535" }
        echo("🌍 Starting CodeContext Server on http://$host:$port")
        embeddedServer(Netty, host = host, port = port, module = Application::module).start(wait = true)
    }
}
