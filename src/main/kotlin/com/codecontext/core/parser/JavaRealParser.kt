package com.codecontext.core.parser

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import java.io.File

class JavaRealParser : LanguageParser {
    override fun parse(file: File): ParsedFile {
        try {
            val cu: CompilationUnit = StaticJavaParser.parse(file)

            val packageName = cu.packageDeclaration.map { it.nameAsString }.orElse("")

            val imports =
                    cu.imports.map {
                        // Handle static imports and normal imports
                        // For graph building, we typically care about the type being imported
                        it.nameAsString
                    }

            // Extract Javadoc from the primary type
            var description = ""
            cu.primaryType.ifPresent { type ->
                type.javadoc.ifPresent { javadoc -> description = javadoc.description.toText() }
            }

            return ParsedFile(file, packageName, imports, description = description)
        } catch (e: Exception) {
            // Fallback or log error. For now, return empty.
            // In a real tool we might want to log this.
            System.err.println("Failed to parse Java file ${file.name}: ${e.message}")
            return ParsedFile(file, "", emptyList())
        }
    }
}
