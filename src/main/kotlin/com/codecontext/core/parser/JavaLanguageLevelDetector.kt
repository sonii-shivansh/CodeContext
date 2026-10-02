package com.codecontext.core.parser

import com.github.javaparser.ParserConfiguration.LanguageLevel
import java.io.File

/** Detects the Java source level declared by common Maven/Gradle project files. */
object JavaLanguageLevelDetector {
    private val versionPatterns = listOf(
        Regex("<maven.compiler.release>\\s*(\\d+)\\s*</maven.compiler.release>"),
        Regex("<maven.compiler.source>\\s*(?:1\\.)?(\\d+)\\s*</maven.compiler.source>"),
        Regex("<maven.compiler.target>\\s*(?:1\\.)?(\\d+)\\s*</maven.compiler.target>"),
        Regex("<java.version>\\s*(?:1\\.)?(\\d+)\\s*</java.version>"),
        Regex("<release>\\s*(\\d+)\\s*</release>"),
        Regex("(?:sourceCompatibility|targetCompatibility)\\s*=\\s*[\\\"'](?:JavaVersion\\.)?VERSION_(?:1_)?(\\d+)[\\\"']"),
        Regex("(?:sourceCompatibility|targetCompatibility)\\s*=\\s*[\\\"'](?:1\\.)?(\\d+)[\\\"']"),
        Regex("JavaLanguageVersion\\.of\\(\\s*(\\d+)\\s*\\)"),
        Regex("jvmToolchain\\(\\s*(\\d+)\\s*\\)"),
        Regex("java(?:\\.version)?\\s*=\\s*(?:1\\.)?(\\d+)")
    )

    private val configurationFiles = listOf(
        "pom.xml",
        "build.gradle",
        "build.gradle.kts",
        "gradle.properties",
        ".sdkmanrc"
    )

    fun detect(file: File): LanguageLevel = languageLevelFor(detectVersion(file))

    fun detectVersion(file: File): Int? {
        var current = file.canonicalFile.parentFile
        while (current != null) {
            for (name in configurationFiles) {
                val config = current.resolve(name)
                if (!config.isFile) continue
                extractVersion(config.readText())?.let { return it }
            }
            current = current.parentFile
        }
        return null
    }

    fun languageLevelFor(version: Int?): LanguageLevel {
        if (version == null) return LanguageLevel.CURRENT
        val normalized = if (version == 1) 8 else version
        return LanguageLevel.values().firstOrNull { it.name == "JAVA_$normalized" }
            ?: LanguageLevel.CURRENT
    }

    private fun extractVersion(content: String): Int? {
        for (pattern in versionPatterns) {
            val match = pattern.find(content) ?: continue
            match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
        }
        return null
    }
}
