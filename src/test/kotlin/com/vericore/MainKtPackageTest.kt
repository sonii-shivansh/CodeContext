package com.vericore

import kotlin.test.Test
import kotlin.test.assertNotNull

class MainKtPackageTest {
    @Test
    fun `application entry point is in the canonical vericore package`() {
        assertNotNull(Class.forName("com.vericore.MainKt"))
    }
}
