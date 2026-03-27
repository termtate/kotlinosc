package org.example.route

import org.example.type.OscBundle
import org.example.type.OscMessage
import org.example.type.OscPacket
import org.example.pattern.OscAddressMatcher

/**
 * 决定[OscPacket]分发到路由[OscRouter]的行为
 */
public enum class DispatchMode {
    /**
     * [OscPacket]会分发到每一个符合address匹配规则的route路由
     */
    ALL_MATCH,
    /**
     * [OscPacket]仅会分发到第一个符合address匹配规则的route路由
     */
    FIRST_MATCH,
}

/**
 * 分发[OscPacket]到各自的route
 *
 * 注意，[OscDispatcher]会立即分发所有[OscPacket]；
 * 如果要根据[OscBundle.timeTag]决定何时调度分发[OscPacket]，使用[OscPacketScheduler]
 */
internal class OscDispatcher(val router: OscRouter) {

    /**
     * @return 根据[OscMessage.address]，命中route的数量
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
     * @return 根据[message]的 [address][OscMessage.address]，命中route的数量
     */
    suspend fun dispatchOscMessage(message: OscMessage, dispatchMode: DispatchMode): Int {
        var hit = 0
        for (route in router.routes) {
            if (OscAddressMatcher.matches(route.compiledPattern, message.address)) {
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