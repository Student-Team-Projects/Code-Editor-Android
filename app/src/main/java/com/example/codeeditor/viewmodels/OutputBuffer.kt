package com.example.codeeditor.viewmodels

import com.example.codeeditor.termux.OutputStream

data class OutputChunk(val stream: OutputStream, val text: String)

/**
    Command output that keeps only the most recent [maxChars] characters,
    so a program that prints forever can't run the app out of memory. Thread-safe.
 */
class OutputBuffer(private val maxChars: Int) {
    private val chunks = ArrayDeque<OutputChunk>()
    private var size = 0
    var truncated = false
        private set
    /** Changes on every modification, so callers can skip unchanged snapshots. */
    @Volatile
    var version = 0L
        private set

    @Synchronized
    fun append(stream: OutputStream, text: String) {
        if (text.isEmpty()) return
        // Chunks are stored as they arrive and only merged in snapshot(), so trimming stays cheap.
        chunks.addLast(OutputChunk(stream, text))
        size += text.length
        version++
        trim()
    }

    @Synchronized
    fun snapshot(): List<OutputChunk> {
        val merged = ArrayList<OutputChunk>()
        val text = StringBuilder()
        var stream: OutputStream? = null
        for (chunk in chunks) {
            if (chunk.stream != stream && stream != null) {
                merged.add(OutputChunk(stream, text.toString()))
                text.setLength(0)
            }
            stream = chunk.stream
            text.append(chunk.text)
        }
        if (stream != null) merged.add(OutputChunk(stream, text.toString()))
        return merged
    }

    @Synchronized
    fun clear() {
        chunks.clear()
        size = 0
        truncated = false
        version++
    }

    private fun trim() {
        while (size > maxChars) {
            truncated = true
            val first = chunks.first()
            val excess = size - maxChars
            if (first.text.length <= excess) {
                chunks.removeFirst()
                size -= first.text.length
            } else {
                chunks[0] = OutputChunk(first.stream, first.text.substring(excess))
                size -= excess
            }
        }
    }
}
