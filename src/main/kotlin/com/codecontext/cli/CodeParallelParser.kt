package com.codecontext.cli

import com.codecontext.core.cache.CacheManager
import com.codecontext.core.parser.ParsedFile
import com.codecontext.core.parser.ParserFactory
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CodeParallelParser(private val cacheManager: CacheManager? = null) {

    suspend fun parseFiles(files: List<File>): List<ParsedFile> = coroutineScope {
        if (files.isEmpty()) return@coroutineScope emptyList()

        val runtime = Runtime.getRuntime()
        val availableMemory = runtime.freeMemory()
        val chunkSize =
            when {
                availableMemory < 256_000_000 -> 25
                availableMemory < 512_000_000 -> 50
                else -> 100
            }

        val processed = AtomicInteger(0)
        val total = files.size

        files.chunked(chunkSize).flatMap { chunk ->
            chunk.map { file ->
                async(Dispatchers.IO) {
                    try {
                        cacheManager?.getCachedParse(file)?.let { cached ->
                            val count = processed.incrementAndGet()
                            if (count % 100 == 0 || count == total) {
                                println("   Progress: $count/$total files")
                            }
                            return@async cached
                        }

                        val parser = ParserFactory.getParser(file)
                        val parsed = parser.parse(file)

                        cacheManager?.saveParse(file, parsed)

                        val count = processed.incrementAndGet()
                        if (count % 100 == 0 || count == total) {
                            println("   Progress: $count/$total files")
                        }

                        parsed
                    } catch (e: Exception) {
                        println("⚠️  Failed to parse ${file.name}: ${e.message}")
                        null
                    }
                }
            }.awaitAll()
        }.filterNotNull()
    }
}
