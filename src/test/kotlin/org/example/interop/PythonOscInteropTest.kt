package org.example.interop

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.example.arg.OscInt32
import org.example.arg.OscString
import org.example.route.DispatchMode
import org.example.route.OscRouter
import org.example.transport.OscClient
import org.example.transport.OscServer
import org.example.type.OscMessage
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("interop")
class PythonOscInteropTest {
    @Test
    fun `kotlin osc client should send message to python osc server`() = runBlocking {
        assumeTrue(hasUv(), "uv is not installed; skipping python interop test")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val port = getAvailablePort()
        val outFile = Files.createTempFile("python-osc-server", ".json").toFile()
        val readyDir = Files.createTempDirectory("python-osc-ready")
        val readyFile = readyDir.resolve("ready.flag").toFile()

        try {
            val pyServer = startProcess(
                listOf(
                    "uv", "run", "--with", "python-osc",
                    "python", "src/test/python/py_osc_server_once.py",
                    "--bind", "0.0.0.0",
                    "--port", port.toString(),
                    "--path", "/interop/from_kotlin",
                    "--out", outFile.absolutePath,
                    "--ready", readyFile.absolutePath,
                    "--timeout-ms", "5000"
                )
            )
            waitForFile(readyFile, 8_000)

            val client = OscClient(InetSocketAddress("127.0.0.1", port), scope)
            client.send(OscMessage(address = "/interop/from_kotlin"))
            client.stop()

            val pyExit = waitForProcess(pyServer, 8_000)
            assertEquals(0, pyExit.first, pyExit.second)

            val json = outFile.readText()
            assertTrue(json.contains("\"received\": true"), json)
            assertTrue(json.contains("\"address\": \"/interop/from_kotlin\""), json)
            assertTrue(json.contains("\"args\": []"), json)
        } finally {
            scope.cancel()
            outFile.delete()
            readyFile.delete()
            readyDir.toFile().delete()
        }
    }

    @Test
    fun `python osc client should send message to kotlin osc server`() = runBlocking {
        assumeTrue(hasUv(), "uv is not installed; skipping python interop test")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val port = getAvailablePort()
        val received = CompletableDeferred<OscMessage>()
        val router = OscRouter().apply {
            on("/interop/from_python") { message ->
                received.complete(message)
            }
        }
        val server = OscServer(
            bindAddress = InetSocketAddress("127.0.0.1", port),
            router = router,
            dispatchMode = DispatchMode.ALL_MATCH,
            scope = scope
        )

        try {
            server.start()
            val pySender = startProcess(
                listOf(
                    "uv", "run", "--with", "python-osc",
                    "python", "src/test/python/py_osc_send_once.py",
                    "--target-host", "127.0.0.1",
                    "--target-port", port.toString(),
                    "--path", "/interop/from_python",
                    "--int", "11",
                    "--text", "hello"
                )
            )
            val pyExit = waitForProcess(pySender, 8_000)
            assertEquals(0, pyExit.first, pyExit.second)

            val message = withTimeout(4_000) { received.await() }
            assertEquals("/interop/from_python", message.address)
            assertEquals(2, message.args.size)
            assertEquals(OscInt32(11), message.args[0])
            assertEquals(OscString("hello"), message.args[1])
        } finally {
            server.stop()
            scope.cancel()
        }
    }

    private fun hasUv(): Boolean {
        return try {
            val process = ProcessBuilder("uv", "--version")
                .redirectErrorStream(true)
                .start()
            process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun startProcess(command: List<String>): Process {
        return ProcessBuilder(command)
            .directory(File("."))
            .redirectErrorStream(true)
            .start()
    }

    private fun waitForProcess(process: Process, timeoutMs: Long): Pair<Int, String> {
        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroyForcibly()
            return Pair(-1, "process timeout, command output: ${process.inputStream.bufferedReader().readText()}")
        }
        return Pair(process.exitValue(), process.inputStream.bufferedReader().readText())
    }

    private fun getAvailablePort(): Int = DatagramSocket(0).use { it.localPort }

    private fun waitForFile(file: File, timeoutMs: Long) {
        val start = System.currentTimeMillis()
        while (!file.exists()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                error("timeout waiting for file: ${file.absolutePath}")
            }
            Thread.sleep(20)
        }
    }
}
