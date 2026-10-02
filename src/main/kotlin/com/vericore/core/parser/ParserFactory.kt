package com.vericore.core.parser

import java.io.File

object ParserFactory {
    private val javaParser = JavaRealParser()
    private val kotlinParser = KotlinRegexParser()

    fun getParser(file: File): LanguageParser {
        return when (file.extension) {
            "java" -> javaParser
            "kt" -> kotlinParser
            else -> throw IllegalArgumentException("Unsupported file type: ${file.name}")
        }
    }
}
