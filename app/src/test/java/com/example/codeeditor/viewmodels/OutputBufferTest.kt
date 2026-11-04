package com.example.codeeditor.viewmodels

import com.example.codeeditor.termux.OutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutputBufferTest {
    private fun OutputBuffer.text() = snapshot().joinToString("") { it.text }

    @Test
    fun mergesAdjacentChunksOfTheSameStream() {
        val b = OutputBuffer(100)
        b.append(OutputStream.STDOUT, "a")
        b.append(OutputStream.STDOUT, "b")
        b.append(OutputStream.STDERR, "c")
        b.append(OutputStream.STDOUT, "d")
        assertEquals(
            listOf(
                OutputChunk(OutputStream.STDOUT, "ab"),
                OutputChunk(OutputStream.STDERR, "c"),
                OutputChunk(OutputStream.STDOUT, "d"),
            ),
            b.snapshot()
        )
        assertFalse(b.truncated)
    }

    @Test
    fun keepsOnlyTheMostRecentCharacters() {
        val b = OutputBuffer(10)
        b.append(OutputStream.STDOUT, "0123456")
        b.append(OutputStream.STDERR, "789")
        b.append(OutputStream.STDOUT, "abc")
        assertEquals("3456789abc", b.text())
        assertEquals(OutputStream.STDOUT, b.snapshot().first().stream)
        assertTrue(b.truncated)
    }

    @Test
    fun capHoldsUnderSustainedOutput() {
        val max = TermuxTestVM.MAX_OUTPUT_CHARS
        val b = OutputBuffer(max)
        val line = "y\n".repeat(4096)
        repeat(1000) { b.append(OutputStream.STDOUT, line) }
        b.append(OutputStream.STDOUT, "END")
        val text = b.text()
        assertEquals(max, text.length)
        assertTrue(text.endsWith("END"))
    }

    @Test
    fun clearResets() {
        val b = OutputBuffer(3)
        b.append(OutputStream.STDOUT, "abcdef")
        b.clear()
        assertEquals(emptyList<OutputChunk>(), b.snapshot())
        assertFalse(b.truncated)
    }
}
