package com.example.codeeditor.termux.ssh

import android.content.Context
import android.content.SharedPreferences
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.security.PublicKey

/**
    Trust-on-first-use host key store. Each "host:port" maps to "<key type> <fingerprint>".
 */
class HostKeyStore(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    fun stored(host: String, port: Int): String? = prefs.getString(entry(host, port), null)

    fun reset(host: String, port: Int) {
        prefs.edit().remove(entry(host, port)).apply()
    }

    fun verifier() = Verifier()

    /** One per connection attempt; remembers what happened so the caller can report it. */
    inner class Verifier : HostKeyVerifier {
        var acceptedFingerprint: String? = null
            private set
        var mismatch: String? = null
            private set

        override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
            val type = KeyType.fromKey(key).toString()
            val fingerprint = SecurityUtils.getFingerprint(key)
            val known = stored(hostname, port)
            if (known == null) {
                prefs.edit().putString(entry(hostname, port), "$type $fingerprint").apply()
            } else if (known != "$type $fingerprint") {
                mismatch = "expected $known, got $type $fingerprint"
                return false
            }
            acceptedFingerprint = fingerprint
            return true
        }

        // Ask for the stored key type, so a server with several keys offers the one we know.
        override fun findExistingAlgorithms(hostname: String, port: Int): List<String> =
            stored(hostname, port)?.let { listOf(it.substringBefore(' ')) } ?: emptyList()
    }

    private fun entry(host: String, port: Int) = "$host:$port"

    companion object {
        private const val PREFS_NAME = "termux_ssh_host_keys"
    }
}
