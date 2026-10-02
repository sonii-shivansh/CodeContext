#!/usr/bin/env python3
"""Add bounded, deprecated CodeContext compatibility after canonical migration."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def patch(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"Expected migration anchor not found in {path}: {old[:120]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def patch_all(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"Expected migration anchor not found in {path}: {old[:120]!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def main() -> None:
    config = ROOT / "src/main/kotlin/com/vericore/core/config/VericoreConfig.kt"
    patch(config,
        '    private const val AI_PROVIDER = "VERICORE_AI_PROVIDER"\n    private const val AI_MODEL = "VERICORE_AI_MODEL"',
        '    private const val AI_PROVIDER = "VERICORE_AI_PROVIDER"\n    private const val AI_MODEL = "VERICORE_AI_MODEL"\n    private const val LEGACY_AI_PROVIDER = "CODECONTEXT_AI_PROVIDER"\n    private const val LEGACY_AI_MODEL = "CODECONTEXT_AI_MODEL"\n    private const val CANONICAL_CONFIG_FILE = ".vericore.json"\n    private const val LEGACY_CONFIG_FILE = ".codecontext.json"\n\n    private fun warnLegacy(identifier: String, replacement: String) {\n        System.err.println("⚠️ Deprecated $identifier is in use; replace it with $replacement.")\n    }\n\n    private fun resolveConfigFile(file: File): File {\n        if (file.name == CANONICAL_CONFIG_FILE && !file.exists()) {\n            val legacy = File(file.parentFile ?: File("."), LEGACY_CONFIG_FILE)\n            if (legacy.isFile) {\n                warnLegacy(LEGACY_CONFIG_FILE, CANONICAL_CONFIG_FILE)\n                return legacy\n            }\n        }\n        if (file.name == LEGACY_CONFIG_FILE) warnLegacy(LEGACY_CONFIG_FILE, CANONICAL_CONFIG_FILE)\n        return file\n    }\n\n    private fun environment(primary: String, legacy: String): String? {\n        val current = System.getenv(primary)?.trim()?.takeIf { it.isNotEmpty() }\n        if (current != null) return current\n        val old = System.getenv(legacy)?.trim()?.takeIf { it.isNotEmpty() }\n        if (old != null) warnLegacy(legacy, primary)\n        return old\n    }')
    patch(config,
        '        val file = File(configPath)\n        return if (file.exists()) {',
        '        val file = resolveConfigFile(File(configPath))\n        return if (file.exists()) {')
    patch(config,
        '        val provider = System.getenv(AI_PROVIDER)?.trim()?.takeIf { it.isNotEmpty() }\n            ?: if (projectKey == null && user != null) user.provider else project.ai.provider\n        val configuredModel = System.getenv(AI_MODEL)?.trim()?.takeIf { it.isNotEmpty() }\n            ?: if (projectKey == null && user != null) user.model else project.ai.model',
        '        val provider = environment(AI_PROVIDER, LEGACY_AI_PROVIDER)\n            ?: if (projectKey == null && user != null) user.provider else project.ai.provider\n        val configuredModel = environment(AI_MODEL, LEGACY_AI_MODEL)\n            ?: if (projectKey == null && user != null) user.model else project.ai.model')

    user = ROOT / "src/main/kotlin/com/vericore/core/config/UserConfigStore.kt"
    patch(user,
        '    private const val CONFIG_HOME_ENV = "VERICORE_CONFIG_HOME"\n    private const val CONFIG_HOME_PROPERTY = "vericore.config.home"',
        '    private const val CONFIG_HOME_ENV = "VERICORE_CONFIG_HOME"\n    private const val CONFIG_HOME_PROPERTY = "vericore.config.home"\n    private const val LEGACY_CONFIG_HOME_ENV = "CODECONTEXT_CONFIG_HOME"\n    private const val LEGACY_CONFIG_HOME_PROPERTY = "codecontext.config.home"')
    patch(user,
        '    fun load(): UserConfig? {\n        val file = configFile()\n        if (!file.isFile) return null\n        return runCatching { json.decodeFromString<UserConfig>(file.readText()) }.getOrNull()\n    }',
        '    fun load(): UserConfig? {\n        val file = configFile()\n        val legacy = legacyConfigFile()\n        val selected = when {\n            file.isFile -> file\n            legacy.isFile -> {\n                System.err.println("⚠️ Deprecated CodeContext user configuration is in use; migrate to the Vericore configuration location.")\n                legacy\n            }\n            else -> return null\n        }\n        return runCatching { json.decodeFromString<UserConfig>(selected.readText()) }.getOrNull()\n    }')
    patch(user,
        '    private fun configDirectory(): File {\n        val explicitProperty = System.getProperty(CONFIG_HOME_PROPERTY)?.trim().orEmpty()\n        if (explicitProperty.isNotEmpty()) return File(explicitProperty)\n\n        val explicitEnvironment = System.getenv(CONFIG_HOME_ENV)?.trim().orEmpty()\n        if (explicitEnvironment.isNotEmpty()) return File(explicitEnvironment)',
        '    private fun configDirectory(): File {\n        val explicitProperty = System.getProperty(CONFIG_HOME_PROPERTY)?.trim().orEmpty()\n        if (explicitProperty.isNotEmpty()) return File(explicitProperty)\n\n        val legacyProperty = System.getProperty(LEGACY_CONFIG_HOME_PROPERTY)?.trim().orEmpty()\n        if (legacyProperty.isNotEmpty()) {\n            System.err.println("⚠️ Deprecated $LEGACY_CONFIG_HOME_PROPERTY is in use; replace it with $CONFIG_HOME_PROPERTY.")\n            return File(legacyProperty)\n        }\n\n        val explicitEnvironment = System.getenv(CONFIG_HOME_ENV)?.trim().orEmpty()\n        if (explicitEnvironment.isNotEmpty()) return File(explicitEnvironment)\n\n        val legacyEnvironment = System.getenv(LEGACY_CONFIG_HOME_ENV)?.trim().orEmpty()\n        if (legacyEnvironment.isNotEmpty()) {\n            System.err.println("⚠️ Deprecated $LEGACY_CONFIG_HOME_ENV is in use; replace it with $CONFIG_HOME_ENV.")\n            return File(legacyEnvironment)\n        }')
    patch(user,
        '    private fun restrictPermissions(file: File) {',
        '    private fun legacyConfigFile(): File {\n        val os = System.getProperty("os.name", "").lowercase()\n        return when {\n            os.contains("win") -> File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "CodeContext/config.json")\n            os.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support/CodeContext/config.json")\n            else -> File(File(System.getenv("XDG_CONFIG_HOME")?.trim().takeIf { !it.isNullOrEmpty() } ?: File(System.getProperty("user.home"), ".config").absolutePath), "codecontext/config.json")\n        }\n    }\n\n    private fun restrictPermissions(file: File) {')

    server = ROOT / "src/main/kotlin/com/vericore/server/VericoreServer.kt"
    patch(server,
        '        val configured = System.getenv("VERICORE_ALLOWED_PATHS")',
        '        val configured = System.getenv("VERICORE_ALLOWED_PATHS")\n            ?: System.getenv("CODECONTEXT_ALLOWED_PATHS")?.also {\n                System.err.println("⚠️ Deprecated CODECONTEXT_ALLOWED_PATHS is in use; replace it with VERICORE_ALLOWED_PATHS.")\n            }')

    engineering_context = ROOT / "src/main/kotlin/com/vericore/core/intelligence/EngineeringContext.kt"
    patch(engineering_context,
        '        return normalized == ".vericore" || normalized.startsWith(".vericore/") ||\n            normalized == ".vericore-architecture-contract.json" ||',
        '        return normalized == ".vericore" || normalized.startsWith(".vericore/") ||\n            normalized == ".vericore-architecture-contract.json" ||\n            normalized == ".codecontext" || normalized.startsWith(".codecontext/") ||\n            normalized == ".codecontext-architecture-contract.json" ||')

    change_safety = ROOT / "src/main/kotlin/com/vericore/core/workflow/ChangeSafety.kt"
    patch(change_safety,
        '    private val generatedPrefixes = listOf(".vericore/", "output/", "build/", "target/")',
        '    private val generatedPrefixes = listOf(".vericore/", ".codecontext/", "output/", "build/", "target/")')
    patch(change_safety,
        '        return normalized == ".vericore" || generatedPrefixes.any { normalized.startsWith(it) } ||',
        '        return normalized == ".vericore" || normalized == ".codecontext" || generatedPrefixes.any { normalized.startsWith(it) } ||')

    contract = ROOT / "src/main/kotlin/com/vericore/cli/ArchitectureContractCommand.kt"
    patch(contract,
        '        val file = File(contractPath ?: File(root, ".vericore-architecture-contract.json").path)',
        '        val file = contractPath?.let { requested ->\n            val explicit = File(requested)\n            if (explicit.name == ".codecontext-architecture-contract.json") {\n                System.err.println("⚠️ Deprecated .codecontext-architecture-contract.json is in use; use .vericore-architecture-contract.json.")\n            }\n            explicit\n        } ?: File(root, ".vericore-architecture-contract.json").let { canonical ->\n            if (canonical.isFile) canonical else File(root, ".codecontext-architecture-contract.json").takeIf { it.isFile }?.also {\n                System.err.println("⚠️ Deprecated .codecontext-architecture-contract.json is in use; use .vericore-architecture-contract.json.")\n            } ?: canonical\n        }')

    mcp = ROOT / "src/main/kotlin/com/vericore/mcp/McpProtocol.kt"
    patch(mcp,
        '    private fun callTool(id: JsonElement?, params: JsonObject): JsonObject { val name = params["name"]?.jsonPrimitive?.content ?: return errorResponse(id, -32602, "Missing tool name"); val args = params["arguments"]?.jsonObject ?: buildJsonObject {}; return try { resultResponse(id, when (name) {',
        '    private fun callTool(id: JsonElement?, params: JsonObject): JsonObject { val rawName = params["name"]?.jsonPrimitive?.content ?: return errorResponse(id, -32602, "Missing tool name"); val name = if (rawName.startsWith("codecontext_")) { System.err.println("⚠️ Deprecated MCP tool name \'$rawName\' used; use the corresponding vericore_ tool instead."); rawName.replaceFirst("codecontext_", "vericore_") } else rawName; val args = params["arguments"]?.jsonObject ?: buildJsonObject {}; return try { resultResponse(id, when (name) {')

    build = ROOT / "build.gradle.kts"
    patch(build,
        'tasks.jar {\n    manifest {\n        attributes["Main-Class"] = "com.vericore.MainKt"\n    }\n}\n',
        """tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.vericore.MainKt"
    }
}

tasks.named("installDist") {
    doLast {
        val binDir = layout.buildDirectory.dir("install/vericore/bin").get().asFile
        val unixAlias = binDir.resolve("codecontext")
        val dollar = "$"
        unixAlias.writeText(
            "#!/usr/bin/env sh\\n" +
                "echo \\\"⚠️ Deprecated command 'codecontext'. Use 'vericore' instead.\\\" >&2\\n" +
                "exec \\\"${dollar}(dirname \\\"${dollar}0\\\")/vericore\\\" \\\"${dollar}@\\\"\\n"
        )
        unixAlias.setExecutable(true)
        binDir.resolve("codecontext.bat").writeText("@echo off\\necho [deprecated] codecontext is deprecated; use vericore instead. 1>&2\\n\\\"%~dp0vericore.bat\\\" %*\\n")
    }
}
""")

    gitignore = ROOT / ".gitignore"
    text = gitignore.read_text(encoding="utf-8")
    additions = []
    for entry in (".vericore/", ".codecontext/", ".vericore-architecture-contract.json", ".codecontext-architecture-contract.json"):
        if entry not in text.splitlines(): additions.append(entry)
    if additions:
        gitignore.write_text(text.rstrip() + "\n" + "\n".join(additions) + "\n", encoding="utf-8")

    for relative in (
        "scripts/migration/migrate_to_vericore.py",
        "scripts/migration/finalize_vericore_compatibility.py",
        "scripts/migration/test_migrate_to_vericore.py",
        ".github/workflows/vericore-migration-runner.yml",
        ".github/workflows/vericore-migration-executor.yml",
    ):
        target = ROOT / relative
        if target.exists(): target.unlink()


if __name__ == "__main__":
    main()
