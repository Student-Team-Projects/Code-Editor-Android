package com.example.codeeditor.terminal

/** What the terminal does with one input line. */
sealed class TerminalLine {
    /** A bare `cd`, run as [cdCommand] so the terminal can remember the new directory. */
    data class Cd(val args: String) : TerminalLine()
    object Clear : TerminalLine()
    data class Command(val text: String) : TerminalLine()

    companion object {
        // Anything with shell operators (`cd build && make`) is an ordinary command.
        private val bareCd = Regex("""cd(\s+[^;&|<>()`\n]*)?""")

        fun parse(line: String): TerminalLine {
            val trimmed = line.trim()
            return when {
                trimmed == "clear" -> Clear
                bareCd.matches(trimmed) -> Cd(trimmed.removePrefix("cd").trim())
                else -> Command(line)
            }
        }
    }
}

/** Runs `cd` and prints the resulting directory, which becomes the terminal's new cwd. */
fun cdCommand(args: String): String = if (args.isEmpty()) "cd && pwd" else "cd $args && pwd"

/**
    Wraps a user command. Without a PTY, Python buffers its whole output until exit,
    so it is told not to; nothing equivalent exists for C/C++ stdio.
 */
fun terminalCommand(command: String): String = "export PYTHONUNBUFFERED=1\n$command"

// CSI (colours, cursor moves), OSC (window titles), and the remaining two-byte escapes.
private val ansiEscape = Regex("""\u001B\[[0-?]*[ -/]*[@-~]|\u001B\][^\u0007\u001B]*(\u0007|\u001B\\)|\u001B[@-Z\\-_]""")

fun stripAnsi(text: String): String = if (text.indexOf('\u001B') < 0) text else text.replace(ansiEscape, "")

/** Termux's `$HOME`, used only to shorten paths for display. */
const val TERMUX_HOME = "/data/data/com.termux/files/home"

/** `null` (meaning `$HOME`) and paths inside it are shown with `~`. */
fun displayPath(cwd: String?): String = when {
    cwd == null || cwd == TERMUX_HOME -> "~"
    cwd.startsWith("$TERMUX_HOME/") -> "~" + cwd.removePrefix(TERMUX_HOME)
    else -> cwd
}

fun promptFor(cwd: String?): String = displayPath(cwd) + " $"
