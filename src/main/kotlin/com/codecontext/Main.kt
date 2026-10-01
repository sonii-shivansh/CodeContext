package com.codecontext

/** The entry point of the application. Configures the CLI commands and executes the pipeline. */
import com.codecontext.cli.AIAssistantCommand
import com.codecontext.cli.EvolutionCommand
import com.codecontext.cli.ImprovedAnalyzeCommand
import com.codecontext.cli.ImpactCommand
import com.codecontext.cli.MainCommand
import com.codecontext.cli.ServerCommand
import com.github.ajalt.clikt.core.subcommands

/** The entry point of the application. Configures the CLI commands and executes the pipeline. */
fun main(args: Array<String>) {
    try {
        MainCommand()
                .subcommands(
                        ImprovedAnalyzeCommand(),
                        ImpactCommand(),
                        AIAssistantCommand(),
                        EvolutionCommand(),
                        ServerCommand()
                )
                .main(args)
    } catch (e: Throwable) {
        com.codecontext.cli.ErrorHandler.handle(e)
    }
}
