package com.example.codeeditor.termux

/**
    Errors the UI can explain without knowing which backend produced them.
 */
sealed class TermuxException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotReachable(cause: Throwable) : TermuxException("Termux service is not running", cause)
    class AuthFailed(cause: Throwable) : TermuxException("Authentication failed", cause)
    class Untrusted(val detail: String) : TermuxException("Server identity changed: $detail")
    class Failed(cause: Throwable) : TermuxException(cause.message ?: "Command failed", cause)
}
