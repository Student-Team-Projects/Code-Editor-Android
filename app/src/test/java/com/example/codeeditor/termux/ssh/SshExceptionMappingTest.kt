package com.example.codeeditor.termux.ssh

import com.example.codeeditor.termux.TermuxException
import net.schmizz.sshj.common.DisconnectReason
import net.schmizz.sshj.transport.TransportException
import net.schmizz.sshj.userauth.UserAuthException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException

class SshExceptionMappingTest {
    @Test
    fun connectionRefusedIsNotReachable() {
        assertTrue(toTermuxException(ConnectException("refused"), null) is TermuxException.NotReachable)
    }

    @Test
    fun wrappedConnectionRefusedIsNotReachable() {
        val e = IOException("wrapped", ConnectException("refused"))
        assertTrue(toTermuxException(e, null) is TermuxException.NotReachable)
    }

    @Test
    fun authFailure() {
        assertTrue(toTermuxException(UserAuthException("Exhausted"), null) is TermuxException.AuthFailed)
    }

    @Test
    fun hostKeyMismatchUsesVerifierDetail() {
        val e = TransportException(DisconnectReason.HOST_KEY_NOT_VERIFIABLE, "Could not verify")
        val mapped = toTermuxException(e, "expected A, got B")
        assertTrue(mapped is TermuxException.Untrusted)
        assertEquals("expected A, got B", (mapped as TermuxException.Untrusted).detail)
    }

    @Test
    fun hostKeyNotVerifiableWithoutDetail() {
        val e = TransportException(DisconnectReason.HOST_KEY_NOT_VERIFIABLE, "Could not verify")
        assertTrue(toTermuxException(e, null) is TermuxException.Untrusted)
    }

    @Test
    fun anythingElseIsFailed() {
        val mapped = toTermuxException(IOException("boom"), null)
        assertTrue(mapped is TermuxException.Failed)
        assertEquals("boom", mapped.message)
    }

    @Test
    fun termuxExceptionPassesThrough() {
        val e = TermuxException.AuthFailed(RuntimeException())
        assertSame(e, toTermuxException(e, null))
    }
}
