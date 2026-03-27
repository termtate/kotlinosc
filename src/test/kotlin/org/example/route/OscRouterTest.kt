package org.example.route

import kotlinx.coroutines.runBlocking
import org.example.type.OscBundle
import org.example.type.OscMessage
import org.example.arg.toOscTimetag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OscRouterTest {
    @Test
    fun `osc router & dispatcher basic test`() = runBlocking {
        val router = OscRouter()
        val message = OscMessage("/a/b")
        var receiveCount = 0

        router.on("/a/*") {
            receiveCount++
        }

        router.on("/*/b") {
            receiveCount++
        }

        val dispatcher = OscDispatcher(router)

        val hit = dispatcher.dispatch(message)

        assertEquals(2, hit)
        assertEquals(2, receiveCount)
    }

    @Test
    fun `osc router continueOnError test`() = runBlocking {
        val router = OscRouter()
        var receiveCount = 0

        router.on("/silent", continueOnError = true) {
            receiveCount++
            error("")
        }


        router.on("/throw", continueOnError = false) {
            receiveCount++
            error("")
        }

        val dispatcher = OscDispatcher(router)

        assertFailsWith<IllegalStateException> {
            dispatcher.dispatch(OscMessage("/throw"))
        }

        dispatcher.dispatch(OscMessage("/silent"))

        assertEquals(2, receiveCount)
    }

    @Test
    fun `osc dispatcher mode test`() = runBlocking {
        val router = OscRouter()
        val message = OscMessage("/a/b")
        var route1Count = 0
        var route2Count = 0

        router.on("/a/*") {
            route1Count++
        }

        router.on("/*/b") {
            route2Count++
        }

        val dispatcher = OscDispatcher(router)

        val hit = dispatcher.dispatch(message, DispatchMode.ALL_MATCH)
        assertEquals(2, hit)
        assertEquals(1, route1Count)
        assertEquals(1, route2Count)

        val hit2 = dispatcher.dispatch(message, DispatchMode.FIRST_MATCH)
        assertEquals(1, hit2)
        assertEquals(2, route1Count)
        assertEquals(1, route2Count)
    }

    @Test
    fun `osc router route register & unregister by pattern`() = runBlocking {
        val router = OscRouter()
        val message = OscMessage("/a")
        var receiveCount = 0

        router.on("/a") {
            receiveCount++
        }

        router.on("/a") {
            receiveCount++
        }


        val dispatcher = OscDispatcher(router)

        dispatcher.dispatch(message)

        assertEquals(2, receiveCount)

        val deleted = router.off("/a")

        assertEquals(2, deleted)

        dispatcher.dispatch(message)

        assertEquals(2, receiveCount)
    }

    @Test
    fun `osc router route register & unregister by routeId`() = runBlocking {
        val router = OscRouter()
        val message = OscMessage("/a")
        var receiveCount = 0

        val routeId1 = router.on("/a") {
            receiveCount++
        }

        val routeId2 = router.on("/a") {
            receiveCount++
        }


        val dispatcher = OscDispatcher(router)

        dispatcher.dispatch(message)

        assertEquals(2, receiveCount)

        val deleted = router.off(routeId1)

        assertTrue(deleted)

        assertFalse(router.off(routeId1))

        dispatcher.dispatch(message)

        assertEquals(3, receiveCount)

        router.off(routeId2)

        dispatcher.dispatch(message)

        assertEquals(3, receiveCount)
    }

    @Test
    fun `osc dispatcher bundle all match count test`() = runBlocking {
        val router = OscRouter()
        var route1Count = 0
        var route2Count = 0

        router.on("/a/*") { route1Count++ }
        router.on("/*/b") { route2Count++ }

        val bundle = OscBundle(
            timeTag = 1L.toOscTimetag(),
            elements = listOf(
                OscMessage("/a/b"),
                OscMessage("/x/b"),
                OscBundle(
                    timeTag = 1L.toOscTimetag(),
                    elements = listOf(
                        OscMessage("/a/c")
                    )
                )
            )
        )

        val hit = OscDispatcher(router).dispatch(bundle, DispatchMode.ALL_MATCH)

        // /a/b -> 2, /x/b -> 1, /a/c -> 1
        assertEquals(4, hit)
        assertEquals(2, route1Count)
        assertEquals(2, route2Count)
    }

    @Test
    fun `osc dispatcher bundle first match count test`() = runBlocking {
        val router = OscRouter()
        var route1Count = 0
        var route2Count = 0

        router.on("/a/*") { route1Count++ } // first route
        router.on("/*/b") { route2Count++ }

        val bundle = OscBundle(
            timeTag = 1L.toOscTimetag(),
            elements = listOf(
                OscMessage("/a/b"),
                OscMessage("/x/b"),
                OscBundle(
                    timeTag = 1L.toOscTimetag(),
                    elements = listOf(
                        OscMessage("/a/c")
                    )
                )
            )
        )

        val hit = OscDispatcher(router).dispatch(bundle, DispatchMode.FIRST_MATCH)

        // /a/b -> 1(first route), /x/b -> 1(second route), /a/c -> 1(first route)
        assertEquals(3, hit)
        assertEquals(2, route1Count)
        assertEquals(1, route2Count)
    }
}
