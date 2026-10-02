package com.vericore.cli

import com.vericore.core.Version
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.versionOption

class MainCommand(private val commandName: String = "vericore") : CliktCommand(name = commandName) {
    init {
        versionOption(Version.current)
    }

    override fun run() = Unit
}
