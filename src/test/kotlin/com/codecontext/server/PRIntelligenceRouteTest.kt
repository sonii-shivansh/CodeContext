package com.codecontext.server

import kotlin.test.Test
import kotlin.test.assertFailsWith

class PRIntelligenceRouteTest {
    @Test
    fun `working tree mode allows both revisions to be absent`() {
        validateRevisionPair(null, null)
    }

    @Test
    fun `revision mode requires both revisions`() {
        assertFailsWith<IllegalArgumentException> { validateRevisionPair("HEAD~1", null) }
        assertFailsWith<IllegalArgumentException> { validateRevisionPair(null, "HEAD") }
    }

    @Test
    fun `blank or oversized revisions are rejected`() {
        assertFailsWith<IllegalArgumentException> { validateRevisionPair("", "HEAD") }
        assertFailsWith<IllegalArgumentException> { validateRevisionPair("A".repeat(257), "HEAD") }
    }
}
