package com.codecontext.cli

import com.codecontext.core.Version
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.versionOption

class MainCommand : CliktCommand(name = "codecontext") {
    init {
        versionOption(Version.current)
    }

    override fun run() = Unit
}
