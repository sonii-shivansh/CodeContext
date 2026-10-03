package com.vericore.server

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith

class VericoreServerIdentityTest {
    @Test
    fun `server module is compiled under the canonical vericore server identity`() {
        assertNotNull(Class.forName("com.vericore.server.VericoreServerKt"))
        assertFailsWith<ClassNotFoundException> {
            Class.forName("com.vericore.server.CodeContextServerKt")
        }
    }
}
