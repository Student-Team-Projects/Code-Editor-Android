package com.example.codeeditor.termux.ssh

import net.schmizz.sshj.common.Buffer
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.signature.Signature
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Base64

class SshKeyStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun generatedKeyLoadsBack() {
        val store = SshKeyStore(tmp.root)
        assertNull(store.load())
        val pair = store.generate()
        val loaded = store.load()!!
        assertArrayEquals(pair.public.encoded, loaded.public.encoded)
        assertArrayEquals(pair.private.encoded, loaded.private.encoded)
        store.delete()
        assertNull(store.load())
    }

    @Test
    fun sshjTreatsItAsEd25519AndSignsWithIt() {
        val pair = SshKeyStore(tmp.root).generate()
        assertEquals(KeyType.ED25519, KeyType.fromKey(pair.public))
        val signature: Signature = com.hierynomus.sshj.signature.SignatureEdDSA.Factory().create()
        val data = "session data".toByteArray()
        signature.initSign(pair.private)
        signature.update(data)
        // The blob as it goes over the wire: algorithm name, then the signature.
        val wire = Buffer.PlainBuffer().putString("ssh-ed25519").putBytes(signature.encode(signature.sign())).compactData
        signature.initVerify(pair.public)
        signature.update(data)
        assertTrue(signature.verify(wire))
    }

    @Test
    fun publicKeyLineIsInAuthorizedKeysFormat() {
        val pair = SshKeyStore(tmp.root).generate()
        val parts = SshKeyStore.publicKeyLine(pair.public).split(" ")
        assertEquals(listOf("ssh-ed25519", SshKeyStore.KEY_COMMENT), listOf(parts[0], parts[2]))
        val blob = Buffer.PlainBuffer(Base64.getDecoder().decode(parts[1]))
        assertEquals("ssh-ed25519", blob.readString())
        assertEquals(32, blob.readBytes().size)
        assertTrue(SshKeyStore.fingerprint(pair.public).matches(Regex("SHA256:[A-Za-z0-9+/]{43}")))
    }

    @Test
    fun authorizeCommandAddsTheLineOnce() {
        assertEquals(
            "mkdir -p ~/.ssh && chmod 700 ~/.ssh && " +
                "{ grep -qxF 'ssh-ed25519 AAAA x' ~/.ssh/authorized_keys 2>/dev/null || " +
                "echo 'ssh-ed25519 AAAA x' >> ~/.ssh/authorized_keys; } && chmod 600 ~/.ssh/authorized_keys",
            SshKeyStore.authorizeCommand("ssh-ed25519 AAAA x")
        )
    }
}
