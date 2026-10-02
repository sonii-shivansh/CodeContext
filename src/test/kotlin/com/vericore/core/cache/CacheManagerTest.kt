package com.vericore.core.cache

import kotlin.test.Test
import kotlin.test.assertEquals

class CacheManagerTest {
    @Test
    fun `default cache location is canonical Vericore state`() {
        assertEquals(".vericore/cache", CacheManager.DEFAULT_CACHE_DIR)
    }
}
