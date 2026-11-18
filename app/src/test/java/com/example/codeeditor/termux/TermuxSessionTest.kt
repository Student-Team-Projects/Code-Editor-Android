package com.example.codeeditor.termux

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TermuxSessionTest {
    @Test
    fun connectReportsState() = runTest {
        val session = testSession()
        session.connect(FakeTermuxConnection())
        advanceUntilIdle()
        assertEquals(ConnectionState.Connected(mapOf("Fake" to "yes")), session.state.value)
    }

    @Test
    fun connectErrorIsReported() = runTest {
        val session = testSession()
        val error = TermuxException.AuthFailed(RuntimeException())
        session.connect(FakeTermuxConnection(connectError = error))
        advanceUntilIdle()
        assertEquals(ConnectionState.Failed(error), session.state.value)
    }

    @Test
    fun reconnectDisconnectsThePreviousConnection() = runTest {
        val session = testSession()
        val first = FakeTermuxConnection()
        session.connect(first)
        session.connect(FakeTermuxConnection())
        advanceUntilIdle()
        assertTrue(first.disconnected)
    }

    @Test
    fun ensureConnectedReusesALiveConnection() = runTest {
        val session = testSession()
        val conn = FakeTermuxConnection()
        session.connect(conn)
        advanceUntilIdle()
        assertSame(conn, session.ensureConnected())
        assertEquals(1, conn.connects)
    }

    @Test
    fun ensureConnectedReconnectsTheCurrentConnection() = runTest {
        val session = testSession()
        val conn = FakeTermuxConnection()
        session.connect(conn)
        session.disconnect()
        advanceUntilIdle()
        assertSame(conn, session.ensureConnected())
        assertEquals(2, conn.connects)
    }

    @Test
    fun ensureConnectedUsesTheFactoryFirstTime() = runTest {
        val session = testSession()
        val conn = FakeTermuxConnection()
        session.factory = ConnectionFactory { conn }
        assertSame(conn, session.ensureConnected())
        assertSame(conn, session.connection.value)
    }

    @Test
    fun ensureConnectedFailsWithoutAnyWayToConnect() = runTest {
        val session = testSession()
        assertNull(session.ensureConnected())
        session.factory = ConnectionFactory { null }
        assertNull(session.ensureConnected())
    }

    @Test
    fun ensureConnectedReportsAFailedConnect() = runTest {
        val session = testSession()
        val error = TermuxException.NotReachable(RuntimeException())
        session.factory = ConnectionFactory { FakeTermuxConnection(connectError = error) }
        assertNull(session.ensureConnected())
        advanceUntilIdle()
        assertEquals(ConnectionState.Failed(error), session.state.value)
    }
}
