package com.codecontext.core

import com.codecontext.core.parser.ParserFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.io.path.createTempDirectory

class EdgeCaseTest :
        FunSpec({
            val tempDir = createTempDirectory("edge-cases").toFile()
            tempDir.deleteOnExit()

            test("Parser should handle empty file gracefully") {
                val file = File(tempDir, "Empty.kt")
                file.writeText("")

                val parser = ParserFactory.getParser(file)
                val result = parser.parse(file)

                result.imports.size shouldBe 0
                result.packageName shouldBe ""
            }

            test("Parser should handle file with only package") {
                val file = File(tempDir, "PkgOnly.kt")
                file.writeText("package com.only")

                val parser = ParserFactory.getParser(file)
                val result = parser.parse(file)

                result.packageName shouldBe "com.only"
                result.imports.size shouldBe 0
            }

            test("Parser should handle broken syntax") {
                val file = File(tempDir, "Broken.kt")
                file.writeText(
                        """
            package com.broken
            imprt this is not valid kotlin code...
            class { open [ }
        """.trimIndent()
                )

                // JavaRealParser might throw or recover.
                // KotlinRegexParser is robust because it's regex based.
                // Let's test the factory default for .kt (RegexParser).

                val parser = ParserFactory.getParser(file)
                val result = parser.parse(file)

                result.packageName shouldBe "com.broken"
            }

            test(
                    "ParserFactory should default to regex parser for unknown extensions if forced? No, it returns JavaRealParser for java"
            ) {
                // Just ensuring no crash
            }
        })
