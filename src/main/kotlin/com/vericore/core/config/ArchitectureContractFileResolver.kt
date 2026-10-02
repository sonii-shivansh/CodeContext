package com.vericore.core.config

import java.io.File

/** Resolves the canonical Vericore architecture contract with a narrow legacy fallback. */
object ArchitectureContractFileResolver {
    data class Resolution(
        val file: File,
        val usedLegacy: Boolean
    )

    fun resolve(root: File): Resolution {
        val canonical = root.resolve(".vericore-architecture-contract.json")
        if (canonical.exists()) return Resolution(canonical, usedLegacy = false)

        val legacy = root.resolve(".codecontext-architecture-contract.json")
        if (legacy.exists()) return Resolution(legacy, usedLegacy = true)

        return Resolution(canonical, usedLegacy = false)
    }
}
