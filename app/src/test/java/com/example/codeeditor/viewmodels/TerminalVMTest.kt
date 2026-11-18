package com.example.codeeditor.viewmodels

import com.example.codeeditor.terminal.ChunkStyle
import com.example.codeeditor.terminal.OutputChunk
import com.example.codeeditor.terminal.TERMUX_HOME
import com.example.codeeditor.termux.ConnectionFactory
import com.example.codeeditor.termux.ExecRequest
import com.example.codeeditor.termux.ExecResult
import com.example.codeeditor.termux.FakeTermuxConnection
import com.example.codeeditor.termux.OutputSink
import com.example.codeeditor.termux.OutputStream
import com.example.codeeditor.termux.TermuxException
import com.example.codeeditor.termux.TermuxSession
import com.example.codeeditor.termux.testSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalVMTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.connectedVm(
        script: suspend (ExecRequest, OutputSink) -> ExecResult = { _, _ -> ExecResult(0, null, false) },
    ): Pair<TerminalVM, FakeTermuxConnection> {
        val session = testSession()
        val conn = FakeTermuxConnection(script = script)
        session.connect(conn)
        return TerminalVM(session) to conn
    }

    private fun TerminalVM.send(line: String) {
        setInput(line)
        submit()
    }

    private fun TerminalVM.text(style: ChunkStyle? = null) =
        output.value.filter { style == null || it.style == style }.joinToString("") { it.text }

    @Test
    fun runsCommandAndShowsItsOutput() = runTest(dispatcher) {
        val (vm, conn) = connectedVm { _, out ->
            out.onOutput(OutputStream.STDOUT, "out")
            out.onOutput(OutputStream.STDERR, "err\n")
            ExecResult(3, null, false)
        }
        advanceUntilIdle()
        vm.send("make")
        advanceUntilIdle()

        assertEquals(ExecRequest("export PYTHONUNBUFFERED=1\nmake", null, null), conn.requests.single())
        assertEquals(
            listOf(
                OutputChunk(ChunkStyle.COMMAND, "~ $ make\n"),
                OutputChunk(ChunkStyle.OUTPUT, "out"),
                OutputChunk(ChunkStyle.ERROR, "err\n"),
                OutputChunk(ChunkStyle.INFO, "exit 3\n"),
            ),
            vm.output.value
        )
        assertEquals("", vm.input.value)
        assertFalse(vm.running.value)
    }

    @Test
    fun successNeedsNoStatusLineButOutputGetsALineBreak() = runTest(dispatcher) {
        val (vm, _) = connectedVm { _, out ->
            out.onOutput(OutputStream.STDOUT, "\u001B[32mno newline\u001B[0m")
            ExecResult(0, null, false)
        }
        vm.send("printf x")
        advanceUntilIdle()
        assertEquals("~ $ printf x\nno newline\n", vm.text())
    }

    @Test
    fun cdChangesTheWorkingDirectory() = runTest(dispatcher) {
        val (vm, conn) = connectedVm { req, out ->
            if (req.command == "cd proj && pwd") out.onOutput(OutputStream.STDOUT, "$TERMUX_HOME/proj\n")
            ExecResult(0, null, false)
        }
        vm.send("cd proj")
        advanceUntilIdle()
        assertEquals("$TERMUX_HOME/proj", vm.cwd.value)

        vm.send("ls")
        advanceUntilIdle()
        assertEquals("$TERMUX_HOME/proj", conn.requests.last().cwd)
        assertEquals("~ $ cd proj\n~/proj $ ls\n", vm.text(ChunkStyle.COMMAND))
    }

    @Test
    fun failedCdKeepsTheWorkingDirectory() = runTest(dispatcher) {
        val (vm, _) = connectedVm { _, out ->
            out.onOutput(OutputStream.STDERR, "cd: nope: No such file or directory\n")
            ExecResult(1, null, false)
        }
        vm.send("cd nope")
        advanceUntilIdle()
        assertNull(vm.cwd.value)
        assertEquals("cd: nope: No such file or directory\n", vm.text(ChunkStyle.ERROR))
        assertEquals("exit 1\n", vm.text(ChunkStyle.INFO))
    }

    @Test
    fun clearEmptiesTheTranscript() = runTest(dispatcher) {
        val (vm, conn) = connectedVm()
        vm.send("ls")
        advanceUntilIdle()
        vm.send("clear")
        advanceUntilIdle()
        assertEquals(emptyList<OutputChunk>(), vm.output.value)
        assertEquals(1, conn.requests.size)
    }

    @Test
    fun withoutAConnectionItSaysSo() = runTest(dispatcher) {
        val vm = TerminalVM(testSession())
        vm.send("ls")
        advanceUntilIdle()
        assertTrue(vm.text(ChunkStyle.ERROR).startsWith("Not connected."))
    }

    @Test
    fun connectsFirstWhenItCan() = runTest(dispatcher) {
        val session = testSession()
        val conn = FakeTermuxConnection()
        session.factory = ConnectionFactory { conn }
        val vm = TerminalVM(session)
        vm.send("ls")
        advanceUntilIdle()
        assertEquals(1, conn.connects)
        assertEquals(1, conn.requests.size)
    }

    @Test
    fun failedConnectShowsTheReason() = runTest(dispatcher) {
        val session = testSession()
        val error = TermuxException.NotReachable(RuntimeException())
        session.factory = ConnectionFactory { FakeTermuxConnection(connectError = error) }
        val vm = TerminalVM(session)
        vm.send("ls")
        advanceUntilIdle()
        assertTrue(vm.text(ChunkStyle.ERROR).startsWith("Not connected: ${error.message}"))
    }

    @Test
    fun stopCancelsTheCommand() = runTest(dispatcher) {
        val started = CompletableDeferred<Unit>()
        val (vm, conn) = connectedVm { _, out ->
            out.onOutput(OutputStream.STDOUT, "y")
            started.complete(Unit)
            awaitCancellation()
        }
        vm.send("yes")
        started.await()
        assertTrue(vm.running.value)
        vm.send("ignored while running")
        vm.stop()
        advanceUntilIdle()
        assertTrue(conn.cancelled)
        assertFalse(vm.running.value)
        assertEquals(1, conn.requests.size)
        assertEquals("~ $ yes\ny\nstopped\n", vm.text())
    }

    @Test
    fun execErrorIsShown() = runTest(dispatcher) {
        val (vm, _) = connectedVm { _, _ -> throw TermuxException.Failed(RuntimeException("boom")) }
        vm.send("ls")
        advanceUntilIdle()
        assertEquals("error: boom\n", vm.text(ChunkStyle.ERROR))
        assertFalse(vm.running.value)
    }

    @Test
    fun opensTheProjectDirectory() = runTest(dispatcher) {
        val (vm, conn) = connectedVm { req, out ->
            if (req.command == "cd && pwd") out.onOutput(OutputStream.STDOUT, "$TERMUX_HOME\n")
            ExecResult(0, null, false)
        }
        vm.setProjectDirectory("/storage/emulated/0/p")
        vm.send("ls")
        advanceUntilIdle()
        assertEquals(listOf("pwd", "export PYTHONUNBUFFERED=1\nls"), conn.requests.map { it.command })
        assertEquals("/storage/emulated/0/p", conn.requests.last().cwd)

        // The same project again (e.g. after the activity is recreated) doesn't move the terminal.
        vm.send("cd")
        advanceUntilIdle()
        vm.setProjectDirectory("/storage/emulated/0/p")
        assertEquals(TERMUX_HOME, vm.cwd.value)
    }

    @Test
    fun unreachableProjectFallsBackToHome() = runTest(dispatcher) {
        val (vm, conn) = connectedVm { req, out ->
            if (req.command == "pwd") {
                out.onOutput(OutputStream.STDERR, "cd: Permission denied\n")
                ExecResult(1, null, false)
            } else ExecResult(0, null, false)
        }
        vm.setProjectDirectory("/storage/emulated/0/p")
        vm.send("ls")
        advanceUntilIdle()
        assertNull(vm.cwd.value)
        assertNull(conn.requests.last().cwd)
        assertTrue(vm.text(ChunkStyle.INFO).contains("termux-setup-storage"))
    }

    @Test
    fun historyWalksSentCommands() = runTest(dispatcher) {
        val (vm, _) = connectedVm()
        vm.send("a")
        advanceUntilIdle()
        vm.send("b")
        advanceUntilIdle()
        vm.setInput("draft")
        vm.historyPrevious()
        assertEquals("b", vm.input.value)
        vm.historyPrevious()
        assertEquals("a", vm.input.value)
        vm.historyNext()
        vm.historyNext()
        assertEquals("draft", vm.input.value)
    }
}
