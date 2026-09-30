package com.codecontext.core.intelligence

import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic engineering-risk signals.
 *
 * This deliberately does not use an LLM. Risk signals must remain reproducible
 * in CI and provide evidence that a future AI layer can explain.
 */
data class EngineeringRisk(
    val path: String,
    val score: Double,
    val level: RiskLevel,
    val reasons: List<String>
)

enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

object EngineeringRiskEngine {
    fun calculate(snapshot: AnalysisSnapshot): List<EngineeringRisk> = snapshot.files.map { file ->
        val reasons = mutableListOf<String>()
        var score = 0.0

        if (file.churn >= 20) {
            score += 30.0
            reasons += "high change frequency"
        } else if (file.churn >= 10) {
            score += 15.0
            reasons += "elevated change frequency"
        }

        if (file.pageRank >= 0.05) {
            score += 35.0
            reasons += "high dependency centrality"
        } else if (file.pageRank >= 0.02) {
            score += 18.0
            reasons += "notable dependency centrality"
        }

        if (file.importCount >= 30) {
            score += 20.0
            reasons += "large dependency surface"
        } else if (file.importCount >= 15) {
            score += 10.0
            reasons += "moderate dependency surface"
        }

        val hotspot = snapshot.hotspots.firstOrNull { it.path == file.path }
        if (hotspot != null && hotspot.dependents >= 10) {
            score += 15.0
            reasons += "many dependent files"
        }

        if (snapshot.architecture.hasCycles && hotspot?.dependencies ?: 0 > 0) {
            score += 10.0
            reasons += "repository contains dependency cycles"
        }

        score = min(100.0, max(0.0, score))
        EngineeringRisk(file.path, score, levelFor(score), reasons.ifEmpty { listOf("no major deterministic risk signal") })
    }.sortedByDescending { it.score }

    private fun levelFor(score: Double): RiskLevel = when {
        score >= 80 -> RiskLevel.CRITICAL
        score >= 60 -> RiskLevel.HIGH
        score >= 30 -> RiskLevel.MEDIUM
        else -> RiskLevel.LOW
    }
}
