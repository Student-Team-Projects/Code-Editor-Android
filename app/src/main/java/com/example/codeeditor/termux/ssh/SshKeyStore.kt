package com.example.codeeditor.termux.ssh

import android.content.Context
import com.example.codeeditor.termux.q
import net.schmizz.sshj.common.Buffer
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
    The app's own ed25519 login key. Stored unencrypted in app-private storage:
    an Android Keystore key couldn't be used through the BouncyCastle provider sshj signs with.
 */
class SshKeyStore(private val dir: File) {
    constructor(context: Context) : this(context.noBackupFilesDir)

    private val privateFile get() = File(dir, "termux_ssh_ed25519")      // PKCS#8
    private val publicFile get() = File(dir, "termux_ssh_ed25519.pub")   // X.509

    fun load(): KeyPair? {
        if (!privateFile.exists() || !publicFile.exists()) return null
        return runCatching {
            BouncyCastle.ensureRegistered()
            val factory = KeyFactory.getInstance(ALGORITHM, PROVIDER)
            KeyPair(
                factory.generatePublic(X509EncodedKeySpec(publicFile.readBytes())),
                factory.generatePrivate(PKCS8EncodedKeySpec(privateFile.readBytes())),
            )
        }.getOrNull()
    }

    /** Creates a new key, replacing any existing one. */
    fun generate(): KeyPair {
        BouncyCastle.ensureRegistered()
        val pair = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER).generateKeyPair()
        dir.mkdirs()
        writeAtomically(privateFile, pair.private.encoded)
        writeAtomically(publicFile, pair.public.encoded)
        return pair
    }

    fun delete() {
        privateFile.delete()
        publicFile.delete()
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val tmp = File(target.path + ".tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) throw java.io.IOException("Can't write $target")
    }

    companion object {
        private const val ALGORITHM = "Ed25519"
        private const val PROVIDER = "BC"
        const val KEY_COMMENT = "code-editor"

        private fun blob(key: PublicKey): ByteArray = Buffer.PlainBuffer().putPublicKey(key).compactData

        /** The `authorized_keys` line, e.g. `ssh-ed25519 AAAA… code-editor`. */
        fun publicKeyLine(key: PublicKey): String =
            "ssh-ed25519 " + Base64.getEncoder().encodeToString(blob(key)) + " " + KEY_COMMENT

        /** Same format as `ssh-keygen -l`: `SHA256:` plus unpadded base64. */
        fun fingerprint(key: PublicKey): String =
            "SHA256:" + Base64.getEncoder().withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(blob(key)))

        /** Shell command that adds [line] to `~/.ssh/authorized_keys` once. */
        fun authorizeCommand(line: String): String {
            val quoted = q(line)
            return "mkdir -p ~/.ssh && chmod 700 ~/.ssh && " +
                "{ grep -qxF $quoted ~/.ssh/authorized_keys 2>/dev/null || echo $quoted >> ~/.ssh/authorized_keys; } && " +
                "chmod 600 ~/.ssh/authorized_keys"
        }
    }
}
