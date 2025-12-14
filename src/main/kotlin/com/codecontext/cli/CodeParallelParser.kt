package com.codecontext.cli

import com.codecontext.core.cache.CacheManager
import com.codecontext.core.parser.ParsedFile
import com.codecontext.core.parser.ParserFactory
import java.io.File
import kotlinx.coroutines.*

class CodeParallelParser(private val cacheManager: CacheManager? = null) {

    suspend fun parseFiles(files: List<File>): List<ParsedFile> = coroutineScope {
        // Chunk to avoid launching too many coroutines at once if files > 10000
        // Use IO dispatcher for file operations
        files.chunked(100).flatMap { chunk ->
            chunk
                    .map { file ->
                        async(Dispatchers.IO) {
                            // Check cache first
                            cacheManager?.getCachedParse(file)?.let {
                                return@async it
                            }

                            // Parse if not cached
                            val parser = ParserFactory.getParser(file)
                            val parsed = parser.parse(file)

                            // Save to cache
                            cacheManager?.saveParse(file, parsed)

                            parsed
                        }
                    }
                    .awaitAll()
        }
    }
}
