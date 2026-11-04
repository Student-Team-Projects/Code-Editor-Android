package com.example.codeeditor.viewmodels

import com.example.codeeditor.termux.ConnectionState
import com.example.codeeditor.termux.ExecRequest
import com.example.codeeditor.termux.ExecResult
import com.example.codeeditor.termux.OutputSink
import com.example.codeeditor.termux.OutputStream
import com.example.codeeditor.termux.TermuxConnection
import com.example.codeeditor.termux.TermuxException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Scripted backend: the VM must work with nothing but the interface. */
private class FakeTermuxConnection(
    private val connectError: TermuxException? = null,
    private val script: suspend (ExecRequest, OutputSink) -> ExecResult = { _, _ -> ExecResult(0, null, false) },
) : TermuxConnection {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state
    val requests = mutableListOf<ExecRequest>()
    var cancelled = false
    var disconnected = false

    override suspend fun connect() {
        _state.value = ConnectionState.Connecting
        connectError?.let {
            _state.value = ConnectionState.Failed(it)
            throw it
        }
        _state.value = ConnectionState.Connected(mapOf("Fake" to "yes"))
    }

    override suspend fun exec(request: ExecRequest, output: OutputSink): ExecResult {
        requests += request
        try {
            return script(request, output)
        } catch (e: kotlinx.coroutines.CancellationException) {
            cancelled = true
            throw e
        }
    }

    override suspend fun disconnect() {
        disconnected = true
        _state.value = ConnectionState.Disconnected
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TermuxTestVMTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun connectReportsState() = runTest(dispatcher) {
        val vm = TermuxTestVM()
        vm.connect(FakeTermuxConnection())
        advanceUntilIdle()
        assertEquals(ConnectionState.Connected(mapOf("Fake" to "yes")), vm.connectionState.value)
    }

    @Test
    fun connectErrorIsReported() = runTest(dispatcher) {
        val vm = TermuxTestVM()
        val error = TermuxException.AuthFailed(RuntimeException())
        vm.connect(FakeTermuxConnection(connectError = error))
        advanceUntilIdle()
        assertEquals(ConnectionState.Failed(error), vm.connectionState.value)
    }

    @Test
    fun streamsOutputAndReportsExit() = runTest(dispatcher) {
        val vm = TermuxTestVM()
        val conn = FakeTermuxConnection { _, out ->
            out.onOutput(OutputStream.STDOUT, "out\n")
            out.onOutput(OutputStream.STDERR, "err\n")
            ExecResult(3, null, false)
        }
        vm.connect(conn)
        advanceUntilIdle()
        vm.run("echo out; echo err >&2; exit 3", "  /home  ", 5)
        advanceUntilIdle()

        assertEquals(ExecRequest("echo out; echo err >&2; exit 3", "/home", 5), conn.requests.single())
        assertEquals(
            listOf(OutputChunk(OutputStream.STDOUT, "out\n"), OutputChunk(OutputStream.STDERR, "err\n")),
            vm.output.value
        )
        val run = vm.lastRun.value!!
        assertEquals(RunOutcome.Finished(ExecResult(3, null, false)), run.outcome)
        assertTrue(run.describe().startsWith("exit 3 · "))
        assertFalse(vm.running.value)
    }

    @Test
    fun blankCwdMeansHome() = runTest(dispatcher) {
        val vm = TermuxTestVM()
        val conn = FakeTermuxConnection()
        vm.connect(conn)
        advanceUntilIdle()
        vm.run("pwd", "   ", 60)
        advanceUntilIdle()
        assertEquals(null, conn.requests.single().cwd)
    }

    @Test
    fun execErrorIsReported() = runTest(dispatcher) {
        val vm = TermuxTestVM()
        val error = TermuxException.NotReachable(RuntimeException())
        vm.connect(FakeTermuxConnection { _, _ -> throw error })
        advanceUntilIdle()
        vm.run("ls", "", 60)
        advanceUntilIdle()
        assertEquals(RunOutcome.Error(error), vm.lastRun.value!!.outcome)
    }

    @Test
    fun cancelStopsTheJob() = runTest(dispatcher) {
        val vm = TermuxTestVM()
        val started = CompletableDeferred<Unit>()
        val conn = FakeTermuxConnection { _, out ->
            out.onOutput(OutputStream.STDOUT, "y\n")
            started.complete(Unit)
            awaitCancellation()
        }
        vm.connect(conn)
        advanceUntilIdle()
        vm.run("yes", "", 60)
        started.await()
        assertTrue(vm.running.value)

        vm.cancel()
        advanceUntilIdle()
        assertTrue(conn.cancelled)
        assertFalse(vm.running.value)
        assertEquals(RunOutcome.Cancelled, vm.lastRun.value!!.outcome)
        assertEquals("y\n", vm.output.value.single().text)
    }

    @Test
    fun reconnectDisconnectsThePreviousConnection() = runTest(dispatcher) {
        val vm = TermuxTestVM()
        val first = FakeTermuxConnection()
        vm.connect(first)
        advanceUntilIdle()
        vm.connect(FakeTermuxConnection())
        advanceUntilIdle()
        assertTrue(first.disconnected)
    }

    @Test
    fun describeFormatsEachOutcome() {
        fun d(r: ExecResult) = RunSummary(RunOutcome.Finished(r), 1500, 60).describe()
        assertEquals("exit 0 · 1.50 s", d(ExecResult(0, null, false)))
        assertEquals("killed by KILL · 1.50 s", d(ExecResult(null, "KILL", false)))
        assertEquals("timed out after 60 s · 1.50 s", d(ExecResult(null, null, true)))
        assertEquals("exit unknown · 1.50 s", d(ExecResult(null, null, false)))
    }
}
