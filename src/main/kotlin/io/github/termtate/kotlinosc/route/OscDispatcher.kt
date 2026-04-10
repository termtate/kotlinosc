package io.github.termtate.kotlinosc.route

import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.type.OscBundle
import io.github.termtate.kotlinosc.type.OscMessage
import io.github.termtate.kotlinosc.type.OscPacket
import io.github.termtate.kotlinosc.pattern.OscAddressMatcher

/**
 * Dispatch strategy when multiple routes match the same OSC address.
 */
public enum class DispatchMode {
    /**
     * Dispatch to all matched routes.
     */
    ALL_MATCH,
    /**
     * Dispatch only to the first matched route.
     */
    FIRST_MATCH,
}

/**
 * Routes incoming packets to handlers registered in [OscRouter].
 */
internal class OscDispatcher(val router: OscRouter, addressPatternConfig: OscConfig.AddressPattern) {
    private val matcher = OscAddressMatcher(addressPatternConfig)

    /**
     * Dispatches an OSC packet recursively.
     *
     * For [OscBundle], each element is dispatched in order.
     * Returns the total number of matched handler invocations.
     */
    suspend fun dispatch(packet: OscPacket, dispatchMode: DispatchMode = DispatchMode.ALL_MATCH): Int {
        return when (packet) {
            is OscMessage -> dispatchOscMessage(packet, dispatchMode)
            is OscBundle -> {
                var hit = 0
                for (e in packet.elements) {
                    hit += dispatch(e, dispatchMode)
                }
                hit
            }
        }
    }

    /**
     * Dispatches an [OscMessage] and returns match count.
     */
    suspend fun dispatchOscMessage(message: OscMessage, dispatchMode: DispatchMode): Int {
        var hit = 0
        for (route in router.routes) {
            if (matcher.matches(route.compiledPattern, message.address)) {
                route.handler(message)
                hit++
                if (dispatchMode == DispatchMode.FIRST_MATCH) {
                    break
                }
            }
        }
        return hit
    }
}
