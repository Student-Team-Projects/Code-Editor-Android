package com.example.codeeditor.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommandHistoryTest {
    @Test
    fun walksBackAndForwardAndRestoresTheDraft() {
        val h = CommandHistory()
        h.add("a")
        h.add("b")
        assertEquals("b", h.previous("dra"))
        assertEquals("a", h.previous("b"))
        assertNull(h.previous("a"))
        assertEquals("b", h.next())
        assertEquals("dra", h.next())
        assertNull(h.next())
    }

    @Test
    fun skipsBlankAndRepeatedCommands() {
        val h = CommandHistory()
        h.add("ls")
        h.add("ls")
        h.add("  ")
        assertEquals("ls", h.previous(""))
        assertNull(h.previous("ls"))
    }

    @Test
    fun addingEndsTheWalk() {
        val h = CommandHistory()
        h.add("a")
        h.add("b")
        h.previous("")
        h.add("c")
        assertEquals("c", h.previous(""))
    }

    @Test
    fun keepsOnlyTheNewestEntries() {
        val h = CommandHistory(maxEntries = 2)
        listOf("a", "b", "c").forEach(h::add)
        assertEquals("c", h.previous(""))
        assertEquals("b", h.previous("c"))
        assertNull(h.previous("b"))
    }
}
