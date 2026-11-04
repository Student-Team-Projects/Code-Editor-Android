package com.example.codeeditor.termux.ssh

data class SshConfig(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
    val username: String = DEFAULT_USERNAME,
    val password: String,
) {
    override fun toString() = "SshConfig(host=$host, port=$port, username=$username)"

    companion object {
        const val DEFAULT_HOST = "127.0.0.1"
        const val DEFAULT_PORT = 8022
        const val DEFAULT_USERNAME = "termux"
    }
}
