package com.vericore.core.parser

import com.github.javaparser.ParserConfiguration.LanguageLevel
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JavaLanguageLevelDetectorTest {
    @Test
    fun `detects Java version from Maven release`() {
        val root = Files.createTempDirectory("vericore-java-level-").toFile()
        try {
            root.resolve("pom.xml").writeText(
                """
                <project>
                  <properties>
                    <maven.compiler.release>17</maven.compiler.release>
                  </properties>
                </project>
                """.trimIndent()
            )
            val source = root.resolve("src/main/java/demo/App.java")
            source.parentFile.mkdirs()
            source.writeText("package demo; class App {}")

            assertEquals(17, JavaLanguageLevelDetector.detectVersion(source))
            assertEquals(LanguageLevel.JAVA_17, JavaLanguageLevelDetector.detect(source))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `detects Java toolchain from Gradle Kotlin DSL`() {
        val root = Files.createTempDirectory("vericore-gradle-java-level-").toFile()
        try {
            root.resolve("build.gradle.kts").writeText("java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }")
            val source = root.resolve("src/main/java/demo/App.java")
            source.parentFile.mkdirs()
            source.writeText("package demo; class App {}")

            assertEquals(21, JavaLanguageLevelDetector.detectVersion(source))
            assertEquals(LanguageLevel.JAVA_21, JavaLanguageLevelDetector.detect(source))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `uses current parser level when repository does not declare a Java version`() {
        val root = Files.createTempDirectory("vericore-java-default-").toFile()
        try {
            val source = root.resolve("App.java")
            source.writeText("class App {}")
            assertEquals(LanguageLevel.CURRENT, JavaLanguageLevelDetector.detect(source))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `parses Java 17 instanceof pattern without parser warning`() {
        val root = Files.createTempDirectory("vericore-java-pattern-").toFile()
        try {
            root.resolve("pom.xml").writeText("<maven.compiler.release>17</maven.compiler.release>")
            val source = root.resolve("App.java")
            source.writeText(
                """
                class App {
                    boolean matches(Object value) {
                        return value instanceof String text && !text.isBlank();
                    }
                }
                """.trimIndent()
            )

            val parsed = JavaRealParser().parse(source)
            assertNotNull(parsed)
            assertEquals(null, parsed.parseWarning)
        } finally {
            root.deleteRecursively()
        }
    }
}
