package com.vericore.cli

import com.github.ajalt.clikt.testing.test
import com.vericore.core.Version
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainCommandTest {
    @Test
    fun `version output uses canonical vericore command identity`() {
        val result = MainCommand().test("--version")

        assertEquals(0, result.statusCode)
        assertEquals("vericore version ${Version.current}", result.stdout.trim())
    }

    @Test
    fun `help output uses canonical vericore command identity`() {
        val result = MainCommand().test("--help")

        assertEquals(0, result.statusCode)
        assertTrue(result.stdout.lines().first().startsWith("Usage: vericore"))
    }

    @Test
    fun `architecture contract help uses canonical generated artifact`() {
        val result = MainCommand().test("architecture-contract --help")

        assertEquals(0, result.statusCode)
        assertTrue(result.stdout.contains(".vericore-architecture-contract.json"))
    }
}
