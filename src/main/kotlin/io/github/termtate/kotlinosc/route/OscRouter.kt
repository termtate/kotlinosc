package io.github.termtate.kotlinosc.route

import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.pattern.compile
import io.github.termtate.kotlinosc.util.OscLogger
import io.github.termtate.kotlinosc.util.logger

internal typealias OscMessageHandler = suspend (message: OscMessage) -> Unit

internal data class Route(
    val id: Int,
    val rawPattern: String,
    val continueOnError: Boolean,
    val handler: OscMessageHandler
) {
    val compiledPattern = rawPattern.compile()
}

public class OscRouter : OscLogger {
    override val logTag: String
        get() = "OscRouter"

    private var nextRouteId = 0

    internal val routes = mutableListOf<Route>()

    /**
     * Registers a route handler for an OSC address pattern.
     *
     * @return generated route id
     */
    public fun on(
        pattern: String,
        continueOnError: Boolean = true,
        handler: suspend (message: OscMessage) -> Unit
    ): Int {
        val wrappedHandler: OscMessageHandler = { message ->
            if (logger.isTraceEnabled()) {
                logger.trace { "route handler received osc message: $message" }
            }
            if (continueOnError) {
                try {
                    handler(message)
                } catch (e: Exception) {
                    logger.error(e) { "osc route handler error occurred: ${e.message}" }
                }
            } else {
                handler(message)
            }
        }

        logger.debug { "register osc route. pattern: $pattern, continueOnError: $continueOnError" }
        val route = Route(nextRouteId, pattern, continueOnError, wrappedHandler)
        routes += route
        nextRouteId++

        return route.id
    }

    /**
     * @return if any route was removed, return `true`
     */
    public fun off(routeId: Int): Boolean {
        return routes.removeAll { it.id == routeId }.also {
            if (it) {
                logger.debug { "successfully removed osc route by routeId: $routeId" }
            }
        }
    }

    /**
     * Removes all routes whose raw pattern equals [pattern].
     *
     * @return number of removed routes
     */
    public fun off(pattern: String): Int {
        val iterator = routes.iterator()
        var delNum = 0

        while (iterator.hasNext()) {
            if (iterator.next().rawPattern == pattern) {
                iterator.remove()
                delNum++
            }
        }
        logger.debug { "removed osc routes by pattern: $pattern, total removed number: $delNum" }

        return delNum
    }
}


