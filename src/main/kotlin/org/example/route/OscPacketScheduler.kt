package org.example.route

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import org.example.type.OscBundle
import org.example.type.OscMessage
import org.example.type.OscPacket
import org.example.arg.OscTimetag
import org.example.arg.toInstant
import org.example.exception.OscDispatchException
import org.example.exception.OscLifecycleException
import org.example.exception.OscPacketSchedulerException
import org.example.util.OscLogger
import org.example.util.logger
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Schedule and dispatch osc packets by packet type:
 * - OscMessage: dispatch immediately
 * - OscBundle: bundle会被展开为OscMessage(s)并分发。分发时间规则: `effectiveTime = maxOf(parentBundleTimetag, childTimetag)`
 *
 * This scheduler is one-shot: after stop() closes the loop, it cannot be started again.
 *
 * @param maxConcurrentDispatches [dispatch] 允许的并发数。
 * [maxConcurrentDispatches]=1代表[dispatch]按照包的schedule和时间顺序严格串行执行；
 * [maxConcurrentDispatches]>1代表[dispatch]将会不保证顺序并发执行
 *
 */
@OptIn(DelicateCoroutinesApi::class)
internal class OscPacketScheduler private constructor(
    private val scope: CoroutineScope,
    private val dispatch: suspend (message: OscMessage, dispatchMode: DispatchMode) -> Unit,
    private val now: () -> Instant,
    private val continueOnDispatchError: Boolean,
    private val maxConcurrentDispatches: Int = 1,
    private val dispatchDispatcher: CoroutineDispatcher = Dispatchers.IO
) : OscLogger {
    override val logTag: String
        get() = "OscPacketScheduler"

    private data class ScheduledItem(
        val dueAt: Instant,
        val message: OscMessage,
        val dispatchMode: DispatchMode,
        val id: Long,
        val groupId: Long? = null,
        var cancelled: Boolean = false
    )

    private sealed interface Command {
        data class ScheduleReq(
            val packet: OscPacket,
            val dispatchMode: DispatchMode,
            val reply: CompletableDeferred<Long>
        ) : Command

        data class CancelReq(val id: Long, val reply: CompletableDeferred<Boolean>) : Command
        data object Tick : Command
        data object Stop : Command
    }

    private val queue = PriorityQueue<ScheduledItem>(
        compareBy<ScheduledItem> { it.dueAt }.thenBy { it.id }
    )
    private val commands = Channel<Command>(Channel.BUFFERED)
    private val byId = HashMap<Long, ScheduledItem>()
    private val groupToTaskIds = HashMap<Long, MutableSet<Long>>()

    private var loopJob: Job? = null
    private val lifecycleMutex = Mutex()
    private var nextTaskId = 0L

    private var stopped = false

    private val dispatchSemaphore = Semaphore(maxConcurrentDispatches)

    private val inFlightJobs = ConcurrentHashMap.newKeySet<Job>()

    init {
        require(maxConcurrentDispatches >= 1) { "maxConcurrentDispatches must be greater than 0" }
    }

    /** Start event loop. Calling start() after stop() throws. */
    suspend fun start() {
        lifecycleMutex.withLock {
            if (loopJob != null) {
                logger.debug { "eventLoop() already running" }
                return@withLock
            }
            if (stopped) {
                throw OscLifecycleException("OscPacketScheduler.start() cannot be called after stop()")
            }
            loopJob = scope.launch { eventLoop() }
            logger.debug { "eventLoop() start" }
        }
    }

    /**
     * 立即分发或在之后分发[packet]
     *
     * - if [packet] is [OscMessage] -> 立即分发
     * - if [packet] is [OscBundle] -> bundle会被展开为OscMessage(s)并分发。分发时间规则: `effectiveTime = maxOf(parentBundleTimetag, childTimetag)`
     *
     * @return task id of scheduled packet
     */
    suspend fun schedule(packet: OscPacket, dispatchMode: DispatchMode = DispatchMode.ALL_MATCH): Long {
        if (commands.isClosedForSend) {
            throw OscPacketSchedulerException("eventLoop() is closed")
        }
        val req = CompletableDeferred<Long>()
        commands.send(Command.ScheduleReq(packet, dispatchMode, req))
        return req.await()
    }

    /**
     * 取消对应[id]的schedule任务
     *
     * 注意，[OscMessage]和 timetag=[OscTimetag.IMMEDIATELY] 的 [OscBundle] 会被立即分发，因此无法取消
     */
    suspend fun cancel(id: Long): Boolean {
        val req = CompletableDeferred<Boolean>()
        commands.send(Command.CancelReq(id, req))
        return req.await()
    }

    suspend fun tick() {
        commands.send(Command.Tick)
    }

    /** Stop event loop and wait until fully stopped. */
    suspend fun stop() {
        lifecycleMutex.withLock {
            stopped = true
            val job = loopJob ?: return@withLock
            commands.send(Command.Stop)
            job.join()
            loopJob = null

            val remainingInFlight = inFlightJobs.toList()
            remainingInFlight.joinAll()
            inFlightJobs.clear()

            logger.debug { "eventLoop() stopped" }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun eventLoop() {
        while (currentCoroutineContext().isActive) {
            val waitMs = nextWaitMillis(now())

            select<Unit> {
                commands.onReceive { cmd ->
                    logger.debug { "channel received cmd: $cmd" }
                    when (cmd) {
                        is Command.ScheduleReq -> {
                            when (cmd.packet) {
                                is OscMessage -> {
                                    val taskId = nextTaskId++
                                    val item = ScheduledItem(now(), cmd.packet, cmd.dispatchMode, taskId)
                                    byId[taskId] = item
                                    queue.add(item)
                                    cmd.reply.complete(taskId)
                                }
                                is OscBundle -> {
                                    val groupId = nextTaskId++
                                    flattenBundleAndDispatch(cmd.packet, cmd.dispatchMode, groupId)
                                    cmd.reply.complete(groupId)
                                }
                            }
                            dispatchDue(now())
                        }
                        is Command.CancelReq -> {
                            val item = byId[cmd.id]
                            val group = groupToTaskIds[cmd.id]

                            if (item != null) {
                                item.cancelled = true
                                byId.remove(cmd.id)
                                cmd.reply.complete(true)
                            } else if (group != null) {
                                for (id in group) {
                                    byId[id]?.cancelled = true
                                }
                                groupToTaskIds.remove(cmd.id)
                                cmd.reply.complete(true)
                            } else {
                                cmd.reply.complete(false)
                            }
                        }
                        Command.Tick -> dispatchDue(now())
                        Command.Stop -> {
                            commands.close()
                            return@onReceive
                        }
                    }
                }
                if (waitMs != null) {
                    onTimeout(waitMs) {
                        dispatchDue(now())
                    }
                }
            }

            if (commands.isClosedForReceive) break
        }
    }

    private fun nextWaitMillis(now: Instant): Long? {
        val head = queue.peek() ?: return null
        return (head.dueAt - now).inWholeMilliseconds.coerceAtLeast(0L)
    }

    private fun flattenBundleAndDispatch(bundle: OscBundle, dispatchMode: DispatchMode, groupId: Long, parentTimetag: OscTimetag? = null) {
        val effectiveTime = if (parentTimetag != null) {
            maxOf(parentTimetag, bundle.timeTag)
        } else {
            bundle.timeTag
        }
        for (packet in bundle.elements) {
            when (packet) {
                is OscMessage -> {
                    val instant = if (effectiveTime.isImmediately()) now() else effectiveTime.toInstant()
                    val taskId = nextTaskId++
                    val item = ScheduledItem(instant, packet, dispatchMode, taskId, groupId)
                    queue.add(item)
                    byId[taskId] = item
                    groupToTaskIds.getOrPut(groupId) { mutableSetOf() }.add(taskId)
                }
                is OscBundle -> flattenBundleAndDispatch(packet, dispatchMode, groupId, effectiveTime)
            }
        }
    }

    private suspend fun dispatchDue(now: Instant) {
        while (true) {
            val head = queue.peek() ?: break
            if (head.dueAt > now) break
            queue.poll()
            byId.remove(head.id)
            if (head.groupId != null) {
                groupToTaskIds[head.groupId]?.let { set ->
                    set.remove(head.id)
                    if (set.isEmpty()) {
                        groupToTaskIds.remove(head.groupId)
                    }
                }
            }
            if (head.cancelled) {
                logger.debug { "item $head cancelled, skip" }
                continue
            }

            if (maxConcurrentDispatches == 1) {
                dispatchNow(head.message, head.dispatchMode)
            } else {
                dispatchConcurrently(head)
            }
            logger.debug {"consumed item $head from queue"}
        }
    }

    private fun dispatchConcurrently(item: ScheduledItem) {
        val job = scope.launch(dispatchDispatcher) {
            var acquired = false
            try {
                dispatchSemaphore.acquire()
                acquired = true
                dispatchNow(item.message, item.dispatchMode)
                logger.debug { "consumed item $item from queue" }
            } finally {
                if (acquired) {
                    dispatchSemaphore.release()
                }
                inFlightJobs.remove(currentCoroutineContext().job)
            }
        }
        inFlightJobs.add(job)
    }

    private suspend fun dispatchNow(message: OscMessage, dispatchMode: DispatchMode) {
        try {
            dispatch(message, dispatchMode)
            logger.debug { "dispatched packet $message" }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val wrapped = OscDispatchException("dispatch failed, packet dropped: $message", t)
            if (continueOnDispatchError) {
                logger.error(wrapped) { wrapped.message ?: "dispatch failed" }
            } else {
                throw wrapped
            }
        }
    }

    companion object {
        operator fun invoke(dispatcher: OscDispatcher): OscPacketScheduler {
            return OscPacketScheduler(
                CoroutineScope(SupervisorJob() + Dispatchers.Default),
                dispatcher::dispatchOscMessage,
                now = Clock.System::now,
                continueOnDispatchError = true
            )
        }

        fun factory(
            dispatch: suspend (message: OscMessage, dispatchMode: DispatchMode) -> Unit,
            scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            now: () -> Instant = Clock.System::now,
            continueOnDispatchError: Boolean = true,
            maxConcurrentDispatches: Int = 1,
            dispatchDispatcher: CoroutineDispatcher = Dispatchers.IO
        ) = OscPacketScheduler(
            scope = scope,
            dispatch = dispatch,
            now = now,
            continueOnDispatchError = continueOnDispatchError,
            maxConcurrentDispatches = maxConcurrentDispatches,
            dispatchDispatcher = dispatchDispatcher
        )
    }
}
