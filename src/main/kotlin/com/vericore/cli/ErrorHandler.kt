package com.vericore.cli

import com.vericore.core.exceptions.VericoreException
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintHelpMessage
import mu.KotlinLogging

object ErrorHandler {
    private val logger = KotlinLogging.logger {}

    fun handle(e: Throwable) {
        when (e) {
            is PrintHelpMessage -> throw e // Let Clikt handle help
            is CliktError -> throw e // Let Clikt handle validation errors
            is VericoreException -> {
                logger.debug(e) { "Vericore exception occurred: ${e.message}" }
                System.err.println("❌ ${e.message}")
            }
            else -> {
                logger.error(e) { "Unexpected error occurred" }
                System.err.println("❌ An unexpected error occurred: ${e.message}")
                System.err.println("   Check the logs for more details.")
            }
        }
    }
}
