package com.vericore

import com.vericore.cli.AIAssistantCommand
import com.vericore.cli.ArchitectureCommand
import com.vericore.cli.ArchitectureContractCommand
import com.vericore.cli.ArchitectureDriftCommand
import com.vericore.cli.DoctorCommand
import com.vericore.cli.EngineeringContextDiffCommand
import com.vericore.cli.EngineeringContextSnapshotCommand
import com.vericore.cli.EngineeringPlanCommand
import com.vericore.cli.EvolutionCommand
import com.vericore.cli.ImprovedAnalyzeCommand
import com.vericore.cli.ImpactCommand
import com.vericore.cli.MainCommand
import com.vericore.cli.McpCommand
import com.vericore.cli.PRIntelligenceCommand
import com.vericore.cli.PrepareCommand
import com.vericore.cli.RealityCommand
import com.vericore.cli.RepositoryQACommand
import com.vericore.cli.ServerCommand
import com.vericore.cli.SetupCommand
import com.vericore.cli.VerifyCommand
import com.github.ajalt.clikt.core.subcommands
import kotlin.system.exitProcess

/** Temporary compatibility entry point for existing CodeContext scripts. */
fun main(args: Array<String>) {
    System.err.println("⚠️ Deprecated: 'codecontext' is a compatibility command. Use 'vericore' instead. The legacy alias is temporary and will be removed in a future major release.")
    try {
        MainCommand("codecontext")
            .subcommands(
                ImprovedAnalyzeCommand(), ImpactCommand(), ArchitectureCommand(), ArchitectureDriftCommand(),
                ArchitectureContractCommand(), EngineeringContextSnapshotCommand(), EngineeringContextDiffCommand(),
                RealityCommand(), PRIntelligenceCommand(), RepositoryQACommand(), EngineeringPlanCommand(),
                PrepareCommand(), VerifyCommand(), AIAssistantCommand(), EvolutionCommand(), ServerCommand(),
                McpCommand(), SetupCommand(), DoctorCommand()
            )
            .main(args)
    } catch (e: Throwable) {
        com.vericore.cli.ErrorHandler.handle(e)
        exitProcess(1)
    }
}
