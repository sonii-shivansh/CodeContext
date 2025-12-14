package com.codecontext

import com.codecontext.cli.AnalyzeCommand
import com.codecontext.cli.MainCommand
import com.github.ajalt.clikt.core.subcommands

/** The entry point of the application. Configures the CLI commands and executes the pipeline. */
fun main(args: Array<String>) {
    MainCommand().subcommands(AnalyzeCommand()).main(args)
}
