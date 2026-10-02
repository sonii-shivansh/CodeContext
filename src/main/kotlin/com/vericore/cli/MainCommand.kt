package com.vericore.cli

import com.vericore.core.Version
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.versionOption

class MainCommand(private val cliName: String = "vericore") : CliktCommand(name = cliName) {
    init {
        versionOption(Version.current)
    }

    override fun run() = Unit
}
