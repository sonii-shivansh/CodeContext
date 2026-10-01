package com.codecontext

/** The entry point of the application. Configures the CLI commands and executes the pipeline. */
import com.codecontext.cli.AIAssistantCommand
import com.codecontext.cli.ArchitectureCommand
import com.codecontext.cli.ArchitectureContractCommand
import com.codecontext.cli.ArchitectureDriftCommand
import com.codecontext.cli.DoctorCommand
import com.codecontext.cli.EngineeringContextDiffCommand
import com.codecontext.cli.EngineeringContextSnapshotCommand
import com.codecontext.cli.EngineeringPlanCommand
import com.codecontext.cli.EvolutionCommand
import com.codecontext.cli.ImprovedAnalyzeCommand
import com.codecontext.cli.ImpactCommand
import com.codecontext.cli.MainCommand
import com.codecontext.cli.McpCommand
import com.codecontext.cli.PRIntelligenceCommand
import com.codecontext.cli.PrepareCommand
import com.codecontext.cli.RepositoryQACommand
import com.codecontext.cli.ServerCommand
import com.codecontext.cli.SetupCommand
import com.codecontext.cli.VerifyCommand
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) {
    try {
        MainCommand()
            .subcommands(
                ImprovedAnalyzeCommand(),
                ImpactCommand(),
                ArchitectureCommand(),
                ArchitectureDriftCommand(),
                ArchitectureContractCommand(),
                EngineeringContextSnapshotCommand(),
                EngineeringContextDiffCommand(),
                PRIntelligenceCommand(),
                RepositoryQACommand(),
                EngineeringPlanCommand(),
                PrepareCommand(),
                VerifyCommand(),
                AIAssistantCommand(),
                EvolutionCommand(),
                ServerCommand(),
                McpCommand(),
                SetupCommand(),
                DoctorCommand()
            )
            .main(args)
    } catch (e: Throwable) {
        com.codecontext.cli.ErrorHandler.handle(e)
    }
}
