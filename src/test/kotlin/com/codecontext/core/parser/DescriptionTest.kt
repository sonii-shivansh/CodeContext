package com.codecontext.core.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

class DescriptionTest :
        FunSpec({
            test("KotlinRegexParser should extract KDoc description") {
                val file = File.createTempFile("Test", ".kt")
                file.writeText(
                        """
            package com.test
            
            /**
             * This is a test description.
             * It has multiple lines.
             */
            class TestClass { }
        """.trimIndent()
                )

                val parser = KotlinRegexParser()
                val parsed = parser.parse(file)

                parsed.description shouldBe "This is a test description. It has multiple lines."
                file.deleteOnExit()
            }
        })
