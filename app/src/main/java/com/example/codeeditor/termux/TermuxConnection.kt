package com.example.codeeditor.termux

import kotlinx.coroutines.flow.StateFlow

/**
    The app's only way to reach Termux. Implementations hide the backend (SSH, axs, ...).

    Contract every backend keeps:
    - Cancelling the coroutine that runs [exec] kills the remote process.
    - A backend that can't tell stdout from stderr reports everything as [OutputStream.STDOUT].
    - A backend that can't report an exit code returns `exitCode = null`.
    - A `null` cwd means `$HOME`; the timeout is enforced by the backend.
    - The backend picks its own threads, so callers may call from the main dispatcher.
      [OutputSink] may be called from any thread.
    - Every failure is a [TermuxException].
 */
interface TermuxConnection {
    val state: StateFlow<ConnectionState>
    suspend fun connect()
    suspend fun exec(request: ExecRequest, output: OutputSink): ExecResult
    suspend fun disconnect()
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    /** [info] holds backend-specific details for display, e.g. the SSH host key fingerprint. */
    data class Connected(val info: Map<String, String>) : ConnectionState()
    data class Failed(val error: TermuxException) : ConnectionState()
}

data class ExecRequest(val command: String, val cwd: String? = null, val timeoutSec: Long = 60)

enum class OutputStream { STDOUT, STDERR }

fun interface OutputSink {
    fun onOutput(stream: OutputStream, text: String)
}

data class ExecResult(
    val exitCode: Int?,      // null when unknown, killed, or timed out
    val exitSignal: String?,
    val timedOut: Boolean,
)
