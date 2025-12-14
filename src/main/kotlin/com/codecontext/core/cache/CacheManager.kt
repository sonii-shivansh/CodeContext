package com.codecontext.core.cache

import com.codecontext.core.parser.ParsedFile
import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CacheManager(private val cacheDir: File = File(".codecontext/cache")) {

    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    fun getCachedParse(file: File): ParsedFile? {
        val cacheKey = getCacheKey(file)
        val cacheFile = File(cacheDir, "$cacheKey.json")

        if (!cacheFile.exists()) return null

        // Check if source file is newer than cache
        if (file.lastModified() > cacheFile.lastModified()) return null

        return try {
            val json = cacheFile.readText()
            Json.decodeFromString<ParsedFile>(json)
        } catch (e: Exception) {
            // If cache is corrupted or format changed, ignore
            null
        }
    }

    fun saveParse(file: File, parsed: ParsedFile) {
        val cacheKey = getCacheKey(file)
        val cacheFile = File(cacheDir, "$cacheKey.json")

        try {
            val json = Json.encodeToString(parsed)
            cacheFile.writeText(json)
        } catch (e: Exception) {
            // Silent fail - cache is optional
            System.err.println("Failed to cache: ${e.message}")
        }
    }

    private fun getCacheKey(file: File): String {
        val path = file.absolutePath
        return MessageDigest.getInstance("MD5").digest(path.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
    }

    fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
