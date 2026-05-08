package io.github.termtate.kotlinosc.transport

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.github.termtate.kotlinosc.config.OscConfig
import io.github.termtate.kotlinosc.route.DispatchMode
import io.github.termtate.kotlinosc.route.OscRouter
import java.net.SocketAddress

/**
 * Configuration for [OscServer] runtime.
 *
 * @property bindAddress Socket address to bind.
 * @property router Route registry used for dispatch.
 * @property dispatchMode Route dispatch strategy when multiple routes match.
 * @property scope Parent coroutine scope used by server runtime jobs.
 * @property continueOnDispatchError Whether dispatch errors are logged-and-continued (`true`) or fail-fast (`false`).
 * @property maxConcurrentDispatches Maximum number of concurrent message dispatch jobs. Must be >= 1.
 * @property dispatchDispatcher Dispatcher used for concurrent dispatch jobs.
 * @property transportHook Transport-layer error callback hooks.
 * @property codecConfig Codec behavior options.
 * @property addressPatternConfig Address pattern matching behavior options.
 * @property protocol Transport protocol used by the server.
 */
internal data class OscServerOptions(
    val bindAddress: SocketAddress,
    val router: OscRouter,
    val dispatchMode: DispatchMode = DispatchMode.ALL_MATCH,
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    val continueOnDispatchError: Boolean = true,
    val maxConcurrentDispatches: Int = 1,
    val dispatchDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val transportHook: OscTransportHook = OscTransportHook.NOOP,
    val codecConfig: OscConfig.Codec = OscConfig.Codec.default,
    val addressPatternConfig: OscConfig.AddressPattern = OscConfig.AddressPattern.default,
    val protocol: OscTransportProtocol = OscTransportProtocol.Udp
)
