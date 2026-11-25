package com.example.codeeditor.termux.ssh

import java.security.KeyPair

/** Login tries [key] first, then [password] if it isn't empty. */
data class SshConfig(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
    val username: String = DEFAULT_USERNAME,
    val password: String = "",
    val key: KeyPair? = null,
) {
    override fun toString() = "SshConfig(host=$host, port=$port, username=$username, key=${key != null})"

    companion object {
        const val DEFAULT_HOST = "127.0.0.1"
        const val DEFAULT_PORT = 8022
        const val DEFAULT_USERNAME = "termux"
    }
}
