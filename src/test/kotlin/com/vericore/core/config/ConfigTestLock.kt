package com.vericore.core.config

import java.util.concurrent.locks.ReentrantLock

/**
 * Configuration tests override a process-wide system property. Hold this lock from
 * setup through teardown so concurrent test classes cannot overwrite each other's
 * temporary configuration home.
 */
internal object ConfigTestLock {
    val lock = ReentrantLock()
}
