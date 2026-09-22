package com.codecontext.core.ai

import java.net.http.HttpClient
import java.time.Duration

/**
 * Compatibility shim for request timeout configuration.
 *
 * The JDK HttpClient builder exposes a connection timeout, but does not expose a
 * total call timeout. The analyzer currently relies on the connection timeout;
 * total request deadlines should be enforced at the coroutine call sites.
 */
internal fun HttpClient.Builder.callTimeout(@Suppress("UNUSED_PARAMETER") timeout: Duration): HttpClient.Builder = this
