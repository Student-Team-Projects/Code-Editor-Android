package com.example.codeeditor.terminal

/**
    Previously sent commands, newest last, walked with [previous] and [next].
    The line being typed when the walk starts is kept and comes back after the newest entry.
 */
class CommandHistory(private val maxEntries: Int = 100) {
    private val entries = ArrayList<String>()
    private var position = 0   // == entries.size when not walking
    private var draft = ""

    fun add(command: String) {
        if (command.isNotBlank() && entries.lastOrNull() != command) {
            entries.add(command)
            if (entries.size > maxEntries) entries.removeAt(0)
        }
        position = entries.size
        draft = ""
    }

    /** Returns the older entry to show, or null when there is none. */
    fun previous(current: String): String? {
        if (position == 0) return null
        if (position == entries.size) draft = current
        position--
        return entries[position]
    }

    /** Returns the newer entry to show (the draft after the newest), or null when not walking. */
    fun next(): String? {
        if (position >= entries.size) return null
        position++
        return if (position == entries.size) draft else entries[position]
    }
}
