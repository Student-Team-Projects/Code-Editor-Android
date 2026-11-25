package com.example.codeeditor.termux

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Scripted backend: callers must work with nothing but the interface. */
class FakeTermuxConnection(
    private val connectError: TermuxException? = null,
    private val script: suspend (ExecRequest, OutputSink) -> ExecResult = { _, _ -> ExecResult(0, null, false) },
) : TermuxConnection {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state
    val requests = mutableListOf<ExecRequest>()
    var connects = 0
    var cancelled = false
    var disconnected = false

    override suspend fun connect() {
        connects++
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
        } catch (e: CancellationException) {
            cancelled = true
            throw e
        }
    }

    override suspend fun disconnect() {
        disconnected = true
        _state.value = ConnectionState.Disconnected
    }
}
