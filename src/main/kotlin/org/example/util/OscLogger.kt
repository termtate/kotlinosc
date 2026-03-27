package org.example.util

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

internal interface OscLogger {
    val logTag: String
}

internal val OscLogger.logger: KLogger
    get() = KotlinLogging.logger(logTag)