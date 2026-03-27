package org.example.route

import org.example.type.OscMessage
import org.example.pattern.compile
import org.example.util.OscLogger
import org.example.util.logger

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
     * @param continueOnError [handler]遇到异常时是否记录异常并继续. 为 false 时会立即抛出异常
     *
     * @return routeId
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
     * @return 删除的route的数量
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

