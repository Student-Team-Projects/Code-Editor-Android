package com.example.codeeditor.termux

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Builds a connection from saved settings, or returns null when that needs user input (e.g. a password). */
fun interface ConnectionFactory {
    fun create(): TermuxConnection?
}

/**
    The app-wide Termux connection, shared by every screen.
    Connecting and disconnecting run in [scope], so they finish even when the screen that started them is gone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TermuxSession(private val scope: CoroutineScope) {
    private val _connection = MutableStateFlow<TermuxConnection?>(null)
    val connection: StateFlow<TermuxConnection?> = _connection.asStateFlow()

    val state: StateFlow<ConnectionState> = _connection
        .flatMapLatest { it?.state ?: flowOf(ConnectionState.Disconnected) }
        .stateIn(scope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    /** Used by [ensureConnected] when no connection has been made yet. Set by the backend's configuration code. */
    @Volatile
    var factory: ConnectionFactory? = null

    private val lock = Mutex()

    /** Replaces the current connection (disconnecting it) with [newConnection] and connects that. */
    fun connect(newConnection: TermuxConnection): Job = scope.launch {
        lock.withLock {
            val old = _connection.value
            _connection.value = newConnection
            if (old != null && old !== newConnection) runCatching { old.disconnect() }
            runCatching { newConnection.connect() }
        }
    }

    fun disconnect(): Job = scope.launch {
        lock.withLock { _connection.value?.let { runCatching { it.disconnect() } } }
    }

    /**
        Connects if not connected yet: again with the current connection, or with one from [factory].
        Returns the connected connection, or null if that wasn't possible; [state] then says why.
     */
    suspend fun ensureConnected(): TermuxConnection? {
        val job = scope.launch {
            lock.withLock {
                val current = _connection.value
                if (current?.state?.value is ConnectionState.Connected) return@withLock
                val target = current ?: factory?.create() ?: return@withLock
                _connection.value = target
                runCatching { target.connect() }
            }
        }
        job.join()
        return _connection.value?.takeIf { it.state.value is ConnectionState.Connected }
    }

    companion object {
        @JvmStatic
        val shared = TermuxSession(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }
}
