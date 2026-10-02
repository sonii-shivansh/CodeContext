package com.vericore.core.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.io.File

class ParserTest :
        FunSpec({
            test("KotlinRegexParser should find package and imports") {
                val content =
                        """
            package com.example.demo
            
            import java.util.List
            import com.vericore.core.Graph as GraphCore
            import kotlin.io.*
            
            class Demo { }
        """.trimIndent()

                val file = File.createTempFile("Test", ".kt")
                file.writeText(content)

                val parser = KotlinRegexParser()
                val result = parser.parse(file)

                result.packageName shouldBe "com.example.demo"
                result.imports shouldContain "java.util.List"
                result.imports shouldContain "com.vericore.core.Graph"
                result.imports shouldContain "kotlin.io.*"

                file.delete()
            }

            test("ParserFactory should return correct parser") {
                val javaFile = File("Test.java")
                val ktFile = File("Test.kt")

                (ParserFactory.getParser(javaFile) is JavaRealParser) shouldBe true
                (ParserFactory.getParser(ktFile) is KotlinRegexParser) shouldBe true
            }

            test("JavaRealParser should find package and imports") {
                val content =
                        """
                    package com.example.java.demo;
            
                    import java.util.ArrayList;
                    import com.vericore.core.Graph;
            
                    public class JavaDemo {
                        private ArrayList<String> list;
                    }
                """.trimIndent()

                val file = File.createTempFile("TestJava", ".java")
                file.writeText(content)

                val parser = JavaRealParser()
                val result = parser.parse(file)

                result.packageName shouldBe "com.example.java.demo"
                result.imports shouldContain "java.util.ArrayList"
                result.imports shouldContain "com.vericore.core.Graph"

                file.delete()
            }
        })
