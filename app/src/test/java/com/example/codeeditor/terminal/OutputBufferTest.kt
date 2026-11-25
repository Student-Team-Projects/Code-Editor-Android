package com.example.codeeditor.terminal

import com.example.codeeditor.viewmodels.TerminalVM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutputBufferTest {
    private fun OutputBuffer.text() = snapshot().joinToString("") { it.text }

    @Test
    fun mergesAdjacentChunksOfTheSameStyle() {
        val b = OutputBuffer(100)
        b.append(ChunkStyle.OUTPUT, "a")
        b.append(ChunkStyle.OUTPUT, "b")
        b.append(ChunkStyle.ERROR, "c")
        b.append(ChunkStyle.OUTPUT, "d")
        assertEquals(
            listOf(
                OutputChunk(ChunkStyle.OUTPUT, "ab"),
                OutputChunk(ChunkStyle.ERROR, "c"),
                OutputChunk(ChunkStyle.OUTPUT, "d"),
            ),
            b.snapshot()
        )
        assertFalse(b.truncated)
    }

    @Test
    fun keepsOnlyTheMostRecentCharacters() {
        val b = OutputBuffer(10)
        b.append(ChunkStyle.OUTPUT, "0123456")
        b.append(ChunkStyle.ERROR, "789")
        b.append(ChunkStyle.OUTPUT, "abc")
        assertEquals("3456789abc", b.text())
        assertEquals(ChunkStyle.OUTPUT, b.snapshot().first().style)
        assertTrue(b.truncated)
    }

    @Test
    fun capHoldsUnderSustainedOutput() {
        val max = TerminalVM.MAX_OUTPUT_CHARS
        val b = OutputBuffer(max)
        val line = "y\n".repeat(4096)
        repeat(1000) { b.append(ChunkStyle.OUTPUT, line) }
        b.append(ChunkStyle.OUTPUT, "END")
        val text = b.text()
        assertEquals(max, text.length)
        assertTrue(text.endsWith("END"))
    }

    @Test
    fun clearResets() {
        val b = OutputBuffer(3)
        b.append(ChunkStyle.OUTPUT, "abcdef")
        b.clear()
        assertEquals(emptyList<OutputChunk>(), b.snapshot())
        assertFalse(b.truncated)
    }
}
