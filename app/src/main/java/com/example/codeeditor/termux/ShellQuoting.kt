package com.example.codeeditor.termux

/** Single-quotes [s] for a POSIX shell. */
fun q(s: String): String = "'" + s.replace("'", "'\\''") + "'"

/**
    Prefixes [command] with a `cd` into [cwd], for backends without a working-directory field.
    The command is grouped so that none of its lines run when the `cd` fails.
    A leading `~` is expanded to `$HOME`; everything else is taken literally.
 */
fun withCwd(command: String, cwd: String?): String {
    if (cwd.isNullOrEmpty()) return command
    val target = when {
        cwd == "~" -> "\"\$HOME\""
        cwd.startsWith("~/") -> "\"\$HOME\"/" + q(cwd.substring(2))
        else -> q(cwd)
    }
    return "cd -- $target && {\n$command\n}"
}
