package com.codecontext.cli

import com.codecontext.server.module
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

class ServerCommand :
    CliktCommand(name = "server", help = "Start CodeContext as a SaaS API Server") {
    private val host by option("-h", "--host", help = "Host to bind to").default("0.0.0.0")
    private val port by option("-p", "--port", help = "Port to listen on").int().default(8080)

    override fun run() {
        echo("🌍 Starting CodeContext Server on http://$host:$port")
        embeddedServer(Netty, host = host, port = port, module = Application::module).start(wait = true)
    }
}
