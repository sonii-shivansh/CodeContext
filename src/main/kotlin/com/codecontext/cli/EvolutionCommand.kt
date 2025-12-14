
package com.codecontext.cli

import com.codecontext.core.temporal.TemporalAnalyzer
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EvolutionCommand : CliktCommand(
    name = "evolution",
    help = "Analyze codebase evolution over time"
) {
    private val months by option("--months", help = "Months back to analyze").int().default(6)
    private val interval by option("--interval", help = "Days between snapshots").int().default(30)
    
    override fun run() {
        echo("⏳ Starting Temporal Analysis (Time Machine)...")
        echo("   Looking back $months months, every $interval days.")
        
        val repoPath = File(".").absolutePath
        val analyzer = TemporalAnalyzer(repoPath)
        
        try {
            val snapshots = analyzer.analyzeEvolution(months, interval)
            
            echo("\n📈 Evolution Report:")
            echo("------------------------------------------------")
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
            
            snapshots.forEach { s ->
                echo("${formatter.format(s.timestamp)} | ${s.commitHash.take(7)} | Files: ${s.totalFiles} | Lines: ${s.totalLines}")
            }
            
            if (snapshots.isEmpty()) {
                echo("⚠️ No history found. Is this a git repository?")
            } else {
                val growth = if (snapshots.first().totalFiles > 0) {
                    ((snapshots.last().totalFiles - snapshots.first().totalFiles).toDouble() / snapshots.first().totalFiles) * 100
                } else 0.0
                
                echo("\n📊 Net Growth: ${String.format("%.1f", growth)}%")
            }
            
        } catch (e: Exception) {
            echo("❌ Error: ${e.message}")
            e.printStackTrace()
        }
    }
}
