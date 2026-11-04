package com.example.codeeditor.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.codeeditor.termux.ConnectionState
import com.example.codeeditor.termux.ExecRequest
import com.example.codeeditor.termux.ExecResult
import com.example.codeeditor.termux.TermuxConnection
import com.example.codeeditor.termux.TermuxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

sealed class RunOutcome {
    data class Finished(val result: ExecResult) : RunOutcome()
    data class Error(val error: TermuxException) : RunOutcome()
    object Cancelled : RunOutcome()
}

data class RunSummary(val outcome: RunOutcome, val durationMs: Long, val timeoutSec: Long) {
    fun describe(): String {
        val status = when (outcome) {
            is RunOutcome.Finished -> with(outcome.result) {
                when {
                    timedOut -> "timed out after $timeoutSec s"
                    exitCode != null -> "exit $exitCode"
                    exitSignal != null -> "killed by $exitSignal"
                    else -> "exit unknown"
                }
            }
            is RunOutcome.Error -> "error: ${outcome.error.message}"
            RunOutcome.Cancelled -> "cancelled"
        }
        return "$status · %.2f s".format(Locale.ROOT, durationMs / 1000.0)
    }
}

/**
    State of the Termux lab screen.
 */
class TermuxTestVM : ViewModel() {
    private var connection: TermuxConnection? = null
    private var stateJob: Job? = null
    private var runJob: Job? = null
    private val buffer = OutputBuffer(MAX_OUTPUT_CHARS)
    private var publishedVersion = -1L

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _output = MutableStateFlow<List<OutputChunk>>(emptyList())
    val output: StateFlow<List<OutputChunk>> = _output.asStateFlow()

    private val _outputTruncated = MutableStateFlow(false)
    val outputTruncated: StateFlow<Boolean> = _outputTruncated.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _lastRun = MutableStateFlow<RunSummary?>(null)
    val lastRun: StateFlow<RunSummary?> = _lastRun.asStateFlow()

    /** Replaces the current connection (if any) with [newConnection] and connects it. */
    fun connect(newConnection: TermuxConnection) {
        runJob?.cancel()
        val old = connection
        connection = newConnection
        stateJob?.cancel()
        stateJob = viewModelScope.launch {
            newConnection.state.collect { _connectionState.value = it }
        }
        viewModelScope.launch {
            if (old != null && old !== newConnection) {
                runCatching { old.disconnect() }
            }
            try {
                newConnection.connect()
            } catch (e: TermuxException) {
                _connectionState.value = ConnectionState.Failed(e)
            }
        }
    }

    fun disconnect() {
        runJob?.cancel()
        val current = connection ?: return
        viewModelScope.launch { runCatching { current.disconnect() } }
    }

    fun run(command: String, cwd: String, timeoutSec: Long) {
        val current = connection ?: return
        if (runJob?.isActive == true || command.isBlank()) return
        buffer.clear()
        publishOutput()
        _lastRun.value = null
        val request = ExecRequest(command, cwd.trim().ifEmpty { null }, timeoutSec)
        runJob = viewModelScope.launch {
            _running.value = true
            val start = System.nanoTime()
            // Output can arrive faster than the UI can draw it, so publish at a fixed rate.
            val publisher = launch {
                while (true) {
                    delay(PUBLISH_INTERVAL_MS)
                    publishOutput()
                }
            }
            var outcome: RunOutcome = RunOutcome.Cancelled
            try {
                outcome = RunOutcome.Finished(current.exec(request) { stream, text -> buffer.append(stream, text) })
            } catch (e: TermuxException) {
                outcome = RunOutcome.Error(e)
            } finally {
                publisher.cancel()
                publishOutput()
                _lastRun.value = RunSummary(outcome, (System.nanoTime() - start) / 1_000_000, timeoutSec)
                _running.value = false
            }
        }
    }

    fun cancel() {
        runJob?.cancel()
    }

    private fun publishOutput() {
        val version = buffer.version
        if (version == publishedVersion) return
        publishedVersion = version
        _output.value = buffer.snapshot()
        _outputTruncated.value = buffer.truncated
    }

    override fun onCleared() {
        val current = connection ?: return
        // viewModelScope is already cancelled here.
        CoroutineScope(Dispatchers.Default).launch { runCatching { current.disconnect() } }
    }

    companion object {
        const val MAX_OUTPUT_CHARS = 256 * 1024
        private const val PUBLISH_INTERVAL_MS = 100L
    }
}
