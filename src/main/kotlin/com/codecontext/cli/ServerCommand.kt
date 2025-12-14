package com.codecontext.cli

import com.codecontext.server.module
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*

class ServerCommand :
        CliktCommand(name = "serve", help = "Start CodeContext as a SaaS API Server") {
    private val port by option("-p", "--port", help = "Port to listen on").int().default(8080)

    override fun run() {
        echo("🌍 Starting CodeContext Server on port $port...")
        embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
    }
}
